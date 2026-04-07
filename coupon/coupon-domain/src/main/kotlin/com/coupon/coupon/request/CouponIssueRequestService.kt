package com.coupon.coupon.request

import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.request.command.CouponIssueRequestCommand
import com.coupon.coupon.request.criteria.CouponIssueRequestCriteria
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.Lock
import com.coupon.support.logging.logger
import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service

@Service
class CouponIssueRequestService(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val outboxEventService: OutboxEventService,
    private val couponIssueService: CouponIssueService,
) {
    private val log by logger()

    /**
     * Accepted-model entrypoint.
     * The request row and the outbox row are saved in the same transaction so the request cannot be silently lost.
     */
    fun accept(command: CouponIssueRequestCommand.Accept): CouponIssueRequest =
        Tx.writeable {
            val persisted =
                couponIssueRequestRepository.saveIfAbsent(
                    CouponIssueRequestCriteria.Create.of(command),
                )

            if (persisted.created) {
                outboxEventService.publish(CouponIssueRequestOutboxCommandFactory.issueRequested(persisted.request))
            }

            persisted.request
        }

    fun getById(requestId: Long): CouponIssueRequest =
        Tx.readable {
            couponIssueRequestRepository.findById(requestId)
        }

    fun getRequest(
        requestId: Long,
        userId: Long,
    ): CouponIssueRequest =
        Tx.readable {
            val request = couponIssueRequestRepository.findById(requestId)
            if (request.userId != userId) {
                throw ErrorException(ErrorType.FORBIDDEN_ACCESS)
            }

            request
        }

    fun findOldestByStatus(status: CouponIssueRequestStatus): CouponIssueRequest? =
        Tx.readable {
            couponIssueRequestRepository.findOldestByStatus(status)
        }

    /**
     * Called only after the outbox relay receives Kafka broker acknowledgment.
     */
    fun markEnqueuedAfterRelay(requestId: Long): Boolean =
        Tx.writeable {
            couponIssueRequestRepository.markEnqueued(
                requestId = requestId,
                candidateStatuses = setOf(CouponIssueRequestStatus.PENDING),
            )
        }

    /**
     * Final safety net for requests that exhausted Kafka retry + DLQ handling.
     */
    fun markDeadAfterDeliveryFailure(
        requestId: Long,
        failureReason: String,
    ): Boolean =
        Tx.writeable {
            couponIssueRequestRepository.markDead(
                requestId = requestId,
                candidateStatuses = DELIVERY_ACTIVE_STATUSES,
                resultCode = CouponCommandResultCode.UNKNOWN_ERROR,
                failureReason = failureReason,
            )
        }

    /**
     * Kafka consumer entrypoint.
     * It claims an ENQUEUED request, runs the real issuance flow, and converges the request to SUCCEEDED, FAILED, or ENQUEUED(retry).
     */
    fun process(requestId: Long): CouponIssueRequestProcessingResult {
        val initialRequest =
            findRequestOrNull(requestId)
                ?: return CouponIssueRequestProcessingResult.Dead("Coupon issue request $requestId was not found")

        if (initialRequest.isTerminal()) {
            return CouponIssueRequestProcessingResult.Completed(initialRequest, transitioned = false)
        }

        return try {
            Lock.executeWithLockRequiresNew(
                key = "COUPON_ISSUE:${initialRequest.couponId}",
            ) { processWithinLock(requestId) }
        } catch (exception: Exception) {
            requeueAfterRetryableFailure(requestId, exception)
        }
    }

    /**
     * The lock boundary is already owned by the caller.
     * This method only performs the state transition and issuance result handling.
     */
    private fun processWithinLock(requestId: Long): CouponIssueRequestProcessingResult {
        val latestRequest = couponIssueRequestRepository.findById(requestId)
        if (latestRequest.isTerminal()) {
            return CouponIssueRequestProcessingResult.Completed(
                latestRequest,
                transitioned = false,
            )
        }

        val marked =
            couponIssueRequestRepository.markProcessing(
                requestId = requestId,
                candidateStatuses = setOf(CouponIssueRequestStatus.ENQUEUED),
            )
        if (!marked) {
            val current = couponIssueRequestRepository.findById(requestId)
            if (current.isTerminal()) {
                return CouponIssueRequestProcessingResult.Completed(
                    current,
                    transitioned = false,
                )
            }

            return CouponIssueRequestProcessingResult.Retry(
                "Coupon issue request $requestId was not in a processable status",
                current,
            )
        }

        return try {
            val couponIssue =
                couponIssueService.executeIssue(
                    CouponIssueCommand.Issue(
                        couponId = latestRequest.couponId,
                        userId = latestRequest.userId,
                    ),
                )

            val markedSucceeded =
                couponIssueRequestRepository.markSucceeded(
                    requestId = requestId,
                    couponIssueId = couponIssue.id,
                )
            markedSucceeded.requireStateTransition(requestId, "SUCCEEDED")

            CouponIssueRequestProcessingResult.Completed(
                couponIssueRequestRepository.findById(requestId),
                transitioned = true,
            )
        } catch (exception: ErrorException) {
            if (exception.errorType.isRetryable()) {
                throw exception
            }

            val resultCode = exception.errorType.toResultCode()
            val markedFailed =
                couponIssueRequestRepository.markFailed(
                    requestId = requestId,
                    resultCode = resultCode,
                    failureReason = exception.errorType.message,
                )
            markedFailed.requireStateTransition(requestId, "FAILED")

            CouponIssueRequestProcessingResult.Completed(
                couponIssueRequestRepository.findById(requestId),
                transitioned = true,
            )
        }
    }

    /**
     * Retryable failures return the request to ENQUEUED so Kafka can deliver it again later.
     */
    private fun requeueAfterRetryableFailure(
        requestId: Long,
        exception: Exception,
    ): CouponIssueRequestProcessingResult {
        val reason = exception.toRetryReason()
        val requeued =
            runCatching {
                couponIssueRequestRepository.markEnqueuedForRetry(
                    requestId = requestId,
                    lastDeliveryError = reason,
                    candidateStatuses = setOf(CouponIssueRequestStatus.PROCESSING),
                )
            }.getOrDefault(false)

        val latestRequest = findRequestOrNull(requestId)
        if (latestRequest?.isTerminal() == true) {
            return CouponIssueRequestProcessingResult.Completed(latestRequest, transitioned = false)
        }

        if (requeued) {
            return CouponIssueRequestProcessingResult.Retry(
                reason = reason,
                request = latestRequest,
                transitioned = true,
            )
        }

        return CouponIssueRequestProcessingResult.Retry(reason, latestRequest)
    }

    private fun findRequestOrNull(requestId: Long): CouponIssueRequest? =
        try {
            couponIssueRequestRepository.findById(requestId)
        } catch (exception: ErrorException) {
            if (exception.errorType == ErrorType.NOT_FOUND_DATA) {
                log.debug { "Coupon issue request not found: id=$requestId" }
                null
            } else {
                throw exception
            }
        }

    private fun ErrorType.isRetryable(): Boolean = this == ErrorType.LOCK_ACQUISITION_FAILED

    private fun ErrorType.toResultCode(): CouponCommandResultCode =
        when (this) {
            ErrorType.ALREADY_ISSUED_COUPON -> CouponCommandResultCode.ALREADY_ISSUED
            ErrorType.COUPON_OUT_OF_STOCK -> CouponCommandResultCode.OUT_OF_STOCK
            ErrorType.INVALID_COUPON_STATUS -> CouponCommandResultCode.INVALID_STATUS
            ErrorType.FORBIDDEN_COUPON_ISSUE,
            ErrorType.FORBIDDEN_ACCESS,
            -> CouponCommandResultCode.FORBIDDEN
            ErrorType.NOT_FOUND_DATA,
            ErrorType.NOT_FOUND_COUPON,
            -> CouponCommandResultCode.NOT_FOUND
            else -> CouponCommandResultCode.UNKNOWN_ERROR
        }

    private fun Exception.toRetryReason(): String =
        buildString {
            append(this@toRetryReason::class.simpleName ?: "RuntimeException")
            append(": ")
            append(message ?: "Unknown error")
        }

    private fun Boolean.requireStateTransition(
        requestId: Long,
        targetState: String,
    ) = check(this) { "Coupon issue request $requestId was not updated to $targetState" }

    companion object {
        private val DELIVERY_ACTIVE_STATUSES =
            setOf(
                CouponIssueRequestStatus.ENQUEUED,
                CouponIssueRequestStatus.PROCESSING,
            )
    }
}
