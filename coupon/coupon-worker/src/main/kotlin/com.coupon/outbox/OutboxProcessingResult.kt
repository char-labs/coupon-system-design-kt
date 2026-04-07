package com.coupon.outbox

import java.time.Duration

sealed interface OutboxProcessingResult {
    data object Success : OutboxProcessingResult

    data class Retry(
        val reason: String,
        val retryAfter: Duration? = null,
    ) : OutboxProcessingResult

    data class Dead(
        val reason: String,
    ) : OutboxProcessingResult
}
