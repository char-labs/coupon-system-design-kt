package com.coupon.coupon.request

import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.request.command.CouponIssueRequestCommand
import com.coupon.coupon.request.criteria.CouponIssueRequestCriteria
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.Lock
import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.outbox.command.OutboxEventCommand
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service

@Service
class CouponIssueRequestService(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val outboxEventService: OutboxEventService,
    private val couponIssueService: CouponIssueService,
) {
    fun accept(command: CouponIssueRequestCommand.Accept): CouponIssueRequest =
        Tx.writeable {
            val persisted =
                couponIssueRequestRepository.saveIfAbsent(
                    CouponIssueRequestCriteria.Create.of(command),
                )

            if (persisted.created) {
                outboxEventService.publish(
                    OutboxEventCommand.Publish(
                        eventType = CouponOutboxEventType.COUPON_ISSUE_REQUESTED,
                        aggregateType = "COUPON_ISSUE_REQUEST",
                        aggregateId = persisted.request.id.toString(),
                        payloadJson = """{"requestId":${persisted.request.id}}""",
                        dedupeKey = persisted.request.idempotencyKey,
                    ),
                )
            }

            persisted.request
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

    fun process(requestId: Long): CouponIssueRequestProcessingResult {
        val initialRequest =
            runCatching {
                couponIssueRequestRepository.findById(requestId)
            }.getOrElse {
                return CouponIssueRequestProcessingResult.Dead("Coupon issue request $requestId was not found")
            }

        if (initialRequest.isTerminal()) {
            return CouponIssueRequestProcessingResult.Completed(initialRequest)
        }

        return try {
            Lock.executeWithLockRequiresNew(
                key = "COUPON_ISSUE:${initialRequest.couponId}",
            ) {
                val latestRequest = couponIssueRequestRepository.findById(requestId)
                if (latestRequest.isTerminal()) {
                    return@executeWithLockRequiresNew CouponIssueRequestProcessingResult.Completed(latestRequest)
                }

                val marked = couponIssueRequestRepository.markProcessing(requestId)
                if (!marked) {
                    return@executeWithLockRequiresNew CouponIssueRequestProcessingResult.Retry(
                        "Coupon issue request $requestId was not in a processable status",
                    )
                }

                try {
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
                    check(markedSucceeded) {
                        "Coupon issue request $requestId was not updated to SUCCEEDED"
                    }

                    CouponIssueRequestProcessingResult.Completed(
                        couponIssueRequestRepository.findById(requestId),
                    )
                } catch (exception: ErrorException) {
                    val resultCode = exception.errorType.toResultCode()
                    if (exception.errorType.isRetryable()) {
                        throw exception
                    }

                    val markedFailed =
                        couponIssueRequestRepository.markFailed(
                            requestId = requestId,
                            resultCode = resultCode,
                            failureReason = exception.errorType.message,
                        )
                    check(markedFailed) {
                        "Coupon issue request $requestId was not updated to FAILED"
                    }

                    CouponIssueRequestProcessingResult.Completed(
                        couponIssueRequestRepository.findById(requestId),
                    )
                }
            }
        } catch (exception: Exception) {
            CouponIssueRequestProcessingResult.Retry(exception.toRetryReason())
        }
    }

    private fun CouponIssueRequest.isTerminal(): Boolean = status in TERMINAL_STATUSES

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

    companion object {
        private val TERMINAL_STATUSES =
            setOf(
                CouponIssueRequestStatus.SUCCEEDED,
                CouponIssueRequestStatus.FAILED,
                CouponIssueRequestStatus.DEAD,
            )
    }
}
