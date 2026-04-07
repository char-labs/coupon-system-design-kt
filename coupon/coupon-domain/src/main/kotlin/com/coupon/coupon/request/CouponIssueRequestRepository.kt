package com.coupon.coupon.request

import com.coupon.coupon.request.criteria.CouponIssueRequestCriteria
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import java.time.LocalDateTime

interface CouponIssueRequestRepository {
    fun saveIfAbsent(criteria: CouponIssueRequestCriteria.Create): CouponIssueRequestPersistResult

    fun findById(requestId: Long): CouponIssueRequest

    fun findByIdempotencyKey(idempotencyKey: String): CouponIssueRequest?

    fun findOldestByStatus(status: CouponIssueRequestStatus): CouponIssueRequest?

    fun findStaleByStatuses(
        statuses: Set<CouponIssueRequestStatus>,
        updatedBefore: LocalDateTime,
        limit: Int,
    ): List<CouponIssueRequest>

    fun findInconsistentSucceeded(limit: Int): List<CouponIssueRequest>

    fun recoverStuckProcessing(updatedBefore: LocalDateTime): Int

    fun markEnqueued(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus> = setOf(CouponIssueRequestStatus.PENDING),
        enqueuedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean

    fun markEnqueuedForRetry(
        requestId: Long,
        lastDeliveryError: String,
        candidateStatuses: Set<CouponIssueRequestStatus> = setOf(CouponIssueRequestStatus.PROCESSING),
        enqueuedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean

    fun markProcessing(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus> = setOf(CouponIssueRequestStatus.ENQUEUED),
        processingStartedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean

    fun markSucceeded(
        requestId: Long,
        couponIssueId: Long,
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean

    fun markFailed(
        requestId: Long,
        resultCode: CouponCommandResultCode,
        failureReason: String,
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean

    fun markDead(
        requestId: Long,
        resultCode: CouponCommandResultCode,
        failureReason: String,
        candidateStatuses: Set<CouponIssueRequestStatus> =
            setOf(
                CouponIssueRequestStatus.ENQUEUED,
                CouponIssueRequestStatus.PROCESSING,
            ),
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean
}
