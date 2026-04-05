package com.coupon.coupon

import com.coupon.enums.coupon.CouponIssueStatus
import java.time.LocalDateTime

data class CouponIssue(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: CouponIssueStatus,
) {
    data class Detail(
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
}
