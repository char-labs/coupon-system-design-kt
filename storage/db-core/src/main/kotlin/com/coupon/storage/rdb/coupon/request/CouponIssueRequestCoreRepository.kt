package com.coupon.storage.rdb.coupon.request

import com.coupon.coupon.request.CouponIssueRequest
import com.coupon.coupon.request.CouponIssueRequestPersistResult
import com.coupon.coupon.request.CouponIssueRequestRepository
import com.coupon.coupon.request.criteria.CouponIssueRequestCriteria
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.storage.rdb.support.findByIdOrElseThrow
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CouponIssueRequestCoreRepository(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {
    override fun saveIfAbsent(criteria: CouponIssueRequestCriteria.Create): CouponIssueRequestPersistResult {
        val existing = findByIdempotencyKey(criteria.idempotencyKey)
        if (existing != null) {
            return CouponIssueRequestPersistResult(existing, created = false)
        }

        return try {
            val saved =
                couponIssueRequestJpaRepository
                    .saveAndFlush(CouponIssueRequestEntity(criteria))
                    .toCouponIssueRequest()
            CouponIssueRequestPersistResult(saved, created = true)
        } catch (exception: DataIntegrityViolationException) {
            if (isIdempotencyConstraintViolation(exception)) {
                val duplicated =
                    findByIdempotencyKey(criteria.idempotencyKey)
                        ?: throw exception
                CouponIssueRequestPersistResult(duplicated, created = false)
            } else {
                throw exception
            }
        }
    }

    override fun findById(requestId: Long): CouponIssueRequest =
        couponIssueRequestJpaRepository
            .findByIdOrElseThrow(requestId)
            .toCouponIssueRequest()

    override fun findByIdempotencyKey(idempotencyKey: String): CouponIssueRequest? =
        couponIssueRequestJpaRepository.findByIdempotencyKey(idempotencyKey)?.toCouponIssueRequest()

    override fun findOldestByStatus(status: CouponIssueRequestStatus): CouponIssueRequest? =
        couponIssueRequestJpaRepository
            .findFirstByStatusOrderByUpdatedAtAscCreatedAtAsc(status)
            ?.toCouponIssueRequest()

    override fun findStaleByStatuses(
        statuses: Set<CouponIssueRequestStatus>,
        updatedBefore: LocalDateTime,
        limit: Int,
    ): List<CouponIssueRequest> =
        couponIssueRequestJpaRepository
            .findStaleByStatuses(
                statuses = statuses,
                updatedBefore = updatedBefore,
                pageable = PageRequest.of(0, limit),
            ).map(CouponIssueRequestEntity::toCouponIssueRequest)

    override fun findInconsistentSucceeded(limit: Int): List<CouponIssueRequest> =
        couponIssueRequestJpaRepository
            .findInconsistentSucceeded(
                succeededStatus = CouponIssueRequestStatus.SUCCEEDED,
                pageable = PageRequest.of(0, limit),
            ).map(CouponIssueRequestEntity::toCouponIssueRequest)

    override fun recoverStuckProcessing(updatedBefore: LocalDateTime): Int =
        couponIssueRequestJpaRepository.recoverStuckProcessing(
            processingStatus = CouponIssueRequestStatus.PROCESSING,
            enqueuedStatus = CouponIssueRequestStatus.ENQUEUED,
            updatedBefore = updatedBefore,
            lastDeliveryError = "Recovered stale PROCESSING request",
        )

    override fun markEnqueued(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        enqueuedAt: LocalDateTime,
    ): Boolean =
        couponIssueRequestJpaRepository.markEnqueued(
            requestId = requestId,
            candidateStatuses = candidateStatuses,
            enqueuedStatus = CouponIssueRequestStatus.ENQUEUED,
            enqueuedAt = enqueuedAt,
        ) > 0

    override fun markEnqueuedForRetry(
        requestId: Long,
        lastDeliveryError: String,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        enqueuedAt: LocalDateTime,
    ): Boolean =
        couponIssueRequestJpaRepository.markEnqueuedForRetry(
            requestId = requestId,
            candidateStatuses = candidateStatuses,
            enqueuedStatus = CouponIssueRequestStatus.ENQUEUED,
            enqueuedAt = enqueuedAt,
            lastDeliveryError = lastDeliveryError,
        ) > 0

    override fun markProcessing(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        processingStartedAt: LocalDateTime,
    ): Boolean =
        couponIssueRequestJpaRepository.markProcessing(
            requestId = requestId,
            candidateStatuses = candidateStatuses,
            processingStatus = CouponIssueRequestStatus.PROCESSING,
            processingStartedAt = processingStartedAt,
        ) > 0

    override fun markSucceeded(
        requestId: Long,
        couponIssueId: Long,
        processedAt: LocalDateTime,
    ): Boolean =
        couponIssueRequestJpaRepository.markSucceeded(
            requestId = requestId,
            processingStatus = CouponIssueRequestStatus.PROCESSING,
            succeededStatus = CouponIssueRequestStatus.SUCCEEDED,
            couponIssueId = couponIssueId,
            processedAt = processedAt,
        ) > 0

    override fun markFailed(
        requestId: Long,
        resultCode: CouponCommandResultCode,
        failureReason: String,
        processedAt: LocalDateTime,
    ): Boolean =
        couponIssueRequestJpaRepository.markFailed(
            requestId = requestId,
            processingStatus = CouponIssueRequestStatus.PROCESSING,
            failedStatus = CouponIssueRequestStatus.FAILED,
            resultCode = resultCode,
            failureReason = failureReason,
            processedAt = processedAt,
        ) > 0

    override fun markDead(
        requestId: Long,
        resultCode: CouponCommandResultCode,
        failureReason: String,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        processedAt: LocalDateTime,
    ): Boolean =
        couponIssueRequestJpaRepository.markDead(
            requestId = requestId,
            candidateStatuses = candidateStatuses,
            deadStatus = CouponIssueRequestStatus.DEAD,
            resultCode = resultCode,
            failureReason = failureReason,
            processedAt = processedAt,
        ) > 0

    private fun isIdempotencyConstraintViolation(exception: DataIntegrityViolationException): Boolean =
        generateSequence<Throwable>(exception) { it.cause }
            .mapNotNull { it.message }
            .any { message ->
                message.contains("uk_coupon_issue_request_idempotency_key", ignoreCase = true) ||
                    message.contains("t_coupon_issue_request", ignoreCase = true) &&
                    message.contains("idempotency_key", ignoreCase = true)
            }
}
