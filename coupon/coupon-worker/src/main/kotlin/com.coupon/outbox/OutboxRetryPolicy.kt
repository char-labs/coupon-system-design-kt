package com.coupon.outbox

import com.coupon.config.OutboxWorkerProperties
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.pow

@Component
class OutboxRetryPolicy(
    private val outboxWorkerProperties: OutboxWorkerProperties,
    private val clock: Clock,
) {
    fun shouldMarkDead(nextRetryCount: Int): Boolean = nextRetryCount > outboxWorkerProperties.maxRetries

    fun nextAvailableAt(nextRetryCount: Int): LocalDateTime {
        val retryDelay = retryDelay(nextRetryCount)
        val nextInstant = clock.instant().plus(retryDelay)
        return LocalDateTime.ofInstant(nextInstant, clock.zone)
    }

    private fun retryDelay(nextRetryCount: Int): Duration {
        val exponent = (nextRetryCount - 1).coerceAtLeast(0).toDouble()
        val baseDelayMillis =
            outboxWorkerProperties.retry.initialDelay
                .toMillis()
                .toDouble()
        val maxDelayMillis = outboxWorkerProperties.retry.maxDelay.toMillis()
        val exponentialDelayMillis =
            (baseDelayMillis * outboxWorkerProperties.retry.multiplier.pow(exponent))
                .toLong()
                .coerceAtMost(maxDelayMillis)

        return Duration.ofMillis(exponentialDelayMillis)
    }
}
