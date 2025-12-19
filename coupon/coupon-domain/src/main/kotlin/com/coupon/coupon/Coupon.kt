package com.coupon.coupon

import com.coupon.enums.CouponStatus
import com.coupon.enums.CouponType

data class Coupon(
    val id: Long,
    val code: String,
    val name: String,
    val type: CouponType,
    val status: CouponStatus,
)
