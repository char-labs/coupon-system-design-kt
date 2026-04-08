package com.coupon.health

import com.coupon.config.CouponIssueKafkaProperties
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("couponIssueApi")
class CouponIssueApiHealthIndicator(
    private val couponIssueKafkaProperties: CouponIssueKafkaProperties,
) : HealthIndicator {
    override fun health(): Health {
        val builder =
            if (couponIssueKafkaProperties.enabled) {
                Health.up()
            } else {
                Health.outOfService()
            }

        return builder
            .withDetail("enabled", couponIssueKafkaProperties.enabled)
            .withDetail("topic", couponIssueKafkaProperties.topic)
            .withDetail("ackTimeout", couponIssueKafkaProperties.ackTimeout)
            .withDetail("acceptanceMode", "redis-reserve-and-kafka-ack")
            .build()
    }
}
