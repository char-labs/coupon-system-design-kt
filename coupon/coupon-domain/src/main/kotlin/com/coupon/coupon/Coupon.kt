package com.coupon.coupon

import com.coupon.enums.coupon.CouponStatus
import com.coupon.enums.coupon.CouponType

data class Coupon(
    val id: Long,
    val code: String,
    val name: String,
    val type: CouponType,
    val status: CouponStatus,
)
