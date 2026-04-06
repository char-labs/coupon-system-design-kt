package com.coupon.support.outbox

enum class OutboxEventStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    DEAD,
}
