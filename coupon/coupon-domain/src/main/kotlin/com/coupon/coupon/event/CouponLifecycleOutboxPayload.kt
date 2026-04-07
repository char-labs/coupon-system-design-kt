package com.coupon.coupon.event

import java.time.LocalDateTime

data class CouponLifecycleOutboxPayload(
    val couponIssueId: Long,
    val couponId: Long,
    val userId: Long,
    val occurredAt: LocalDateTime,
) {
    companion object {
        fun from(event: CouponLifecycleDomainEvent) =
            CouponLifecycleOutboxPayload(
                couponIssueId = event.couponIssueId,
                couponId = event.couponId,
                userId = event.userId,
                occurredAt = event.occurredAt,
            )
    }
}
