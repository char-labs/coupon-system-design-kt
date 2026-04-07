package com.coupon.coupon.request

data class CouponIssueRequestPersistResult(
    val request: CouponIssueRequest,
    val created: Boolean,
)
