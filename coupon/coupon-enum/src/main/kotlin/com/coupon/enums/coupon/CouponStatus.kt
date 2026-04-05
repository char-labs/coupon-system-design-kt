package com.coupon.enums.coupon

enum class CouponStatus(
    val description: String,
) {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    EXPIRED("만료"),
}
