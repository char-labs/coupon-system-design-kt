package com.coupon.coupon

import com.coupon.enums.CouponIssueStatus
import java.time.LocalDateTime

data class CouponIssueDetail(
    val id: Long,
    val couponId: Long,
    val couponCode: String,
    val couponName: String,
    val userId: Long,
    val status: CouponIssueStatus,
    val issuedAt: LocalDateTime,
    val usedAt: LocalDateTime?,
    val canceledAt: LocalDateTime?,
)
