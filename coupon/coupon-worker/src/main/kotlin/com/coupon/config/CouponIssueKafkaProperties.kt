package com.coupon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "worker.kafka.coupon-issue")
data class CouponIssueKafkaProperties(
    val enabled: Boolean = true,
    val topic: String = "coupon.issue.v1",
    val dlqTopic: String = "coupon.issue.v1.dlq",
    val groupId: String = "coupon-issue-group",
    val dlqGroupId: String = "coupon-issue-dlq-group",
    val concurrency: Int = 3,
    val observationEnabled: Boolean = true,
    val topicPartitions: Int = 3,
    val topicReplicas: Int = 1,
    val retry: Retry = Retry(),
) {
    init {
        require(topic.isNotBlank()) { "worker.kafka.coupon-issue.topic must not be blank" }
        require(dlqTopic.isNotBlank()) { "worker.kafka.coupon-issue.dlq-topic must not be blank" }
        require(groupId.isNotBlank()) { "worker.kafka.coupon-issue.group-id must not be blank" }
        require(dlqGroupId.isNotBlank()) { "worker.kafka.coupon-issue.dlq-group-id must not be blank" }
        require(concurrency > 0) { "worker.kafka.coupon-issue.concurrency must be greater than 0" }
        require(topicPartitions > 0) { "worker.kafka.coupon-issue.topic-partitions must be greater than 0" }
        require(topicReplicas > 0) { "worker.kafka.coupon-issue.topic-replicas must be greater than 0" }
    }

    data class Retry(
        val maxAttempts: Int = 5,
        val initialInterval: Duration = Duration.ofSeconds(1),
        val multiplier: Double = 2.0,
        val maxInterval: Duration = Duration.ofSeconds(30),
    ) {
        init {
            require(maxAttempts > 0) { "worker.kafka.coupon-issue.retry.max-attempts must be greater than 0" }
            require(!initialInterval.isNegative) {
                "worker.kafka.coupon-issue.retry.initial-interval must not be negative"
            }
            require(multiplier >= 1.0) { "worker.kafka.coupon-issue.retry.multiplier must be at least 1.0" }
            require(!maxInterval.isNegative) {
                "worker.kafka.coupon-issue.retry.max-interval must not be negative"
            }
            require(maxInterval >= initialInterval) {
                "worker.kafka.coupon-issue.retry.max-interval must be greater than or equal to initial-interval"
            }
        }
    }
}
