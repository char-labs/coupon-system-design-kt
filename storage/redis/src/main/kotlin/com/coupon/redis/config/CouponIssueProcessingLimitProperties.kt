package com.coupon.redis.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "worker.kafka.coupon-issue.processing-limit")
data class CouponIssueProcessingLimitProperties(
    val enabled: Boolean = true,
    val permitsPerSecond: Long = 100,
) {
    init {
        require(permitsPerSecond > 0) {
            "worker.kafka.coupon-issue.processing-limit.permits-per-second must be greater than 0"
        }
    }
}
