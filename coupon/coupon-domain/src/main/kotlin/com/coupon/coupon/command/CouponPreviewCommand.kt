package com.coupon.coupon.command

data class CouponPreviewCommand(
    val couponId: Long,
    val userId: Long,
    val orderAmount: Long,
)
