package com.coupon.enums.coupon

enum class CouponIssueRequestStatus {
    PENDING,
    ENQUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    DEAD,
    ;

    fun isTerminal(): Boolean = this == SUCCEEDED || this == FAILED || this == DEAD

    fun hasLeftPending(): Boolean = this != PENDING
}
