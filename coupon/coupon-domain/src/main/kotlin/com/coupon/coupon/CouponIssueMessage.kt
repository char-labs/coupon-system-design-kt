package com.coupon.coupon

import java.time.Instant
import java.util.UUID

data class CouponIssueMessage(
    val couponId: Long,
    val userId: Long,
    val requestId: String = UUID.randomUUID().toString(),
    val acceptedAt: Instant = Instant.now(),
)
