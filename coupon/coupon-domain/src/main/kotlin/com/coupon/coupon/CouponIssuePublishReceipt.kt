package com.coupon.coupon

data class CouponIssuePublishReceipt(
    val topic: String,
    val partition: Int,
    val offset: Long,
)
