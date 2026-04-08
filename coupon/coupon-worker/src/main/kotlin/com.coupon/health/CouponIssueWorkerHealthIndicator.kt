package com.coupon.health

import com.coupon.config.CouponIssueKafkaProperties
import com.coupon.config.OutboxWorkerProperties
import com.coupon.redis.config.CouponIssueProcessingLimitProperties
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("couponIssueWorker")
class CouponIssueWorkerHealthIndicator(
    private val couponIssueKafkaProperties: CouponIssueKafkaProperties,
    private val outboxWorkerProperties: OutboxWorkerProperties,
    private val couponIssueProcessingLimitProperties: CouponIssueProcessingLimitProperties,
) : HealthIndicator {
    override fun health(): Health {
        val builder =
            if (couponIssueKafkaProperties.enabled) {
                Health.up()
            } else {
                Health.outOfService()
            }

        return builder
            .withDetail("consumerEnabled", couponIssueKafkaProperties.enabled)
            .withDetail("topic", couponIssueKafkaProperties.topic)
            .withDetail("groupId", couponIssueKafkaProperties.groupId)
            .withDetail("concurrency", couponIssueKafkaProperties.concurrency)
            .withDetail("processingLimitEnabled", couponIssueProcessingLimitProperties.enabled)
            .withDetail("processingLimitPermitsPerSecond", couponIssueProcessingLimitProperties.permitsPerSecond)
            .withDetail("outboxEnabled", outboxWorkerProperties.enabled)
            .build()
    }
}
