package com.coupon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "worker.outbox")
data class OutboxWorkerProperties(
    val enabled: Boolean = true,
    val batchSize: Int = 100,
    val fixedDelay: Duration = Duration.ofMillis(500),
    val initialDelay: Duration = Duration.ZERO,
    val processingTimeout: Duration = Duration.ofMinutes(5),
    val maxRetries: Int = 10,
    val retry: Retry = Retry(),
) {
    init {
        require(batchSize > 0) { "worker.outbox.batch-size must be greater than 0" }
        require(!fixedDelay.isNegative) { "worker.outbox.fixed-delay must not be negative" }
        require(!initialDelay.isNegative) { "worker.outbox.initial-delay must not be negative" }
        require(!processingTimeout.isNegative) { "worker.outbox.processing-timeout must not be negative" }
        require(maxRetries >= 0) { "worker.outbox.max-retries must be 0 or greater" }
    }

    data class Retry(
        val initialDelay: Duration = Duration.ofSeconds(1),
        val maxDelay: Duration = Duration.ofMinutes(5),
        val multiplier: Double = 2.0,
    ) {
        init {
            require(!initialDelay.isNegative) { "worker.outbox.retry.initial-delay must not be negative" }
            require(!maxDelay.isNegative) { "worker.outbox.retry.max-delay must not be negative" }
            require(maxDelay >= initialDelay) { "worker.outbox.retry.max-delay must be greater than or equal to initial-delay" }
            require(multiplier >= 1.0) { "worker.outbox.retry.multiplier must be at least 1.0" }
        }
    }
}
