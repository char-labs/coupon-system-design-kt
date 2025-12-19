package com.coupon.coupon

import com.coupon.enums.CouponIssueStatus

data class CouponIssue(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: CouponIssueStatus,
)
