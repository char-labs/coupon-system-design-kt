package com.coupon.coupon.request

import com.coupon.coupon.request.criteria.CouponIssueRequestCriteria
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import java.time.LocalDateTime

interface CouponIssueRequestRepository {
    fun saveIfAbsent(criteria: CouponIssueRequestCriteria.Create): CouponIssueRequestPersistResult

    fun findById(requestId: Long): CouponIssueRequest

    fun findByIdempotencyKey(idempotencyKey: String): CouponIssueRequest?

    fun markProcessing(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus> = setOf(CouponIssueRequestStatus.PENDING),
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
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean
}
