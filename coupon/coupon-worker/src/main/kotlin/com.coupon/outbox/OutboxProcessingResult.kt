package com.coupon.outbox

sealed interface OutboxProcessingResult {
    data object Success : OutboxProcessingResult

    data class Retry(
        val reason: String,
    ) : OutboxProcessingResult

    data class Dead(
        val reason: String,
    ) : OutboxProcessingResult
}
