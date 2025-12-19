package com.coupon.enums

enum class CouponIssueStatus(
    val description: String,
) {
    ISSUED("발급됨"),
    USED("사용됨"),
    CANCELED("취소됨"),
}
