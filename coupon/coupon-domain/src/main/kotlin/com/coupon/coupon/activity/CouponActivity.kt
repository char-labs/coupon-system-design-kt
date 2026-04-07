package com.coupon.coupon.activity

import com.coupon.enums.coupon.CouponActivityType
import java.time.LocalDateTime

data class CouponActivity(
    val id: Long,
    val couponIssueId: Long,
    val couponId: Long,
    val userId: Long,
    val activityType: CouponActivityType,
    val occurredAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)
