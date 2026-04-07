package com.coupon.health

import com.coupon.config.OutboxWorkerProperties
import com.coupon.outbox.OutboxEventHandlerRegistry
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("outboxWorker")
class OutboxWorkerHealthIndicator(
    private val outboxWorkerProperties: OutboxWorkerProperties,
    private val outboxEventHandlerRegistry: OutboxEventHandlerRegistry,
) : HealthIndicator {
    override fun health(): Health =
        Health
            .up()
            .withDetail("enabled", outboxWorkerProperties.enabled)
            .withDetail("batchSize", outboxWorkerProperties.batchSize)
            .withDetail("maxRetries", outboxWorkerProperties.maxRetries)
            .withDetail("registeredHandlerCount", outboxEventHandlerRegistry.size())
            .withDetail("registeredEventTypes", outboxEventHandlerRegistry.eventTypes())
            .build()
}
