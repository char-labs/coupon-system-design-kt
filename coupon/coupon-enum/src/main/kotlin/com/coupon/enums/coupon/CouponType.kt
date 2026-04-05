package com.coupon.enums.coupon

enum class CouponType(
    val description: String,
) {
    FIXED("정액 할인"),
    PERCENTAGE("정률 할인"),
}
