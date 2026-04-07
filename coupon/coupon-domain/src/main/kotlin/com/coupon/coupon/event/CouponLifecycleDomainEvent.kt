package com.coupon.coupon.event

import java.time.LocalDateTime

sealed interface CouponLifecycleDomainEvent {
    val couponIssueId: Long
    val couponId: Long
    val userId: Long
    val occurredAt: LocalDateTime

    data class Issued(
        override val couponIssueId: Long,
        override val couponId: Long,
        override val userId: Long,
        override val occurredAt: LocalDateTime = LocalDateTime.now(),
    ) : CouponLifecycleDomainEvent

    data class Used(
        override val couponIssueId: Long,
        override val couponId: Long,
        override val userId: Long,
        override val occurredAt: LocalDateTime = LocalDateTime.now(),
    ) : CouponLifecycleDomainEvent

    data class Canceled(
        override val couponIssueId: Long,
        override val couponId: Long,
        override val userId: Long,
        override val occurredAt: LocalDateTime = LocalDateTime.now(),
    ) : CouponLifecycleDomainEvent
}
