package com.coupon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "worker.coupon-issue-request-reconciliation")
data class CouponIssueRequestReconciliationProperties(
    val enabled: Boolean = true,
    val batchSize: Int = 100,
    val fixedDelay: Duration = Duration.ofSeconds(1),
    val initialDelay: Duration = Duration.ofSeconds(1),
    val processingTimeout: Duration = Duration.ofMinutes(5),
    val pendingTimeout: Duration = Duration.ofSeconds(30),
) {
    init {
        require(batchSize > 0) { "worker.coupon-issue-request-reconciliation.batch-size must be greater than 0" }
        require(!fixedDelay.isNegative) { "worker.coupon-issue-request-reconciliation.fixed-delay must not be negative" }
        require(!initialDelay.isNegative) { "worker.coupon-issue-request-reconciliation.initial-delay must not be negative" }
        require(!processingTimeout.isNegative) { "worker.coupon-issue-request-reconciliation.processing-timeout must not be negative" }
        require(!pendingTimeout.isNegative) { "worker.coupon-issue-request-reconciliation.pending-timeout must not be negative" }
    }
}
