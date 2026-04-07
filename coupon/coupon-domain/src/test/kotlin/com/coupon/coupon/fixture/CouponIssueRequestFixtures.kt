package com.coupon.coupon.fixture

import com.coupon.coupon.request.CouponIssueRequest
import com.coupon.enums.coupon.CouponIssueRequestStatus
import java.time.LocalDateTime

internal object CouponIssueRequestFixtures {
    fun pending(
        id: Long,
        couponId: Long,
        userId: Long,
    ): CouponIssueRequest = build(id = id, couponId = couponId, userId = userId)

    fun enqueued(
        id: Long,
        couponId: Long,
        userId: Long,
        enqueuedAt: LocalDateTime,
    ): CouponIssueRequest =
        build(
            id = id,
            couponId = couponId,
            userId = userId,
            status = CouponIssueRequestStatus.ENQUEUED,
            enqueuedAt = enqueuedAt,
        )

    fun build(
        id: Long,
        couponId: Long,
        userId: Long,
        status: CouponIssueRequestStatus = CouponIssueRequestStatus.PENDING,
        enqueuedAt: LocalDateTime? = null,
    ) = CouponIssueRequest(
        id = id,
        couponId = couponId,
        userId = userId,
        idempotencyKey = "coupon:$couponId:user:$userId:action:ISSUE",
        status = status,
        resultCode = null,
        couponIssueId = null,
        failureReason = null,
        enqueuedAt = enqueuedAt,
        processingStartedAt = null,
        deliveryAttemptCount = 0,
        lastDeliveryError = null,
        processedAt = null,
        createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        updatedAt = LocalDateTime.of(2026, 4, 7, 9, 0),
    )
}
