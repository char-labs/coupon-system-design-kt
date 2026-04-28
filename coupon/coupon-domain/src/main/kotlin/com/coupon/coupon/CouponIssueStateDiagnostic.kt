package com.coupon.coupon

data class CouponIssueStateDiagnostic(
    val couponId: Long,
    val initialized: Boolean,
    val occupiedCount: Long,
    val issuedCount: Long,
)
