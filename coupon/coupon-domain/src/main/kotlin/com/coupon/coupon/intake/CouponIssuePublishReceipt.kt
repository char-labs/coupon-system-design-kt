package com.coupon.coupon.intake

data class CouponIssuePublishReceipt(
    val topic: String,
    val partition: Int,
    val offset: Long,
)
