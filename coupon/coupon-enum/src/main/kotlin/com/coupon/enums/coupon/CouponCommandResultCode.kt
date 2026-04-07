package com.coupon.enums.coupon

enum class CouponCommandResultCode {
    ALREADY_ISSUED,
    OUT_OF_STOCK,
    INVALID_STATUS,
    FORBIDDEN,
    NOT_FOUND,
    DUPLICATE_ACTIVITY,
    MALFORMED_EVENT,
    UNKNOWN_ERROR,
}
