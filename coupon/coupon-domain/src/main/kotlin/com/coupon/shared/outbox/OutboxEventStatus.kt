package com.coupon.shared.outbox

enum class OutboxEventStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    DEAD,
}
