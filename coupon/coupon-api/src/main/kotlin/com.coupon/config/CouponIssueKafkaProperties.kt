package com.coupon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "api.kafka.coupon-issue")
data class CouponIssueKafkaProperties(
    val enabled: Boolean = true,
    val topic: String = "coupon.issue.v1",
    val ackTimeout: Duration = Duration.ofSeconds(10),
    val topicPartitions: Int = 3,
    val topicReplicas: Int = 1,
) {
    init {
        require(topic.isNotBlank()) { "api.kafka.coupon-issue.topic must not be blank" }
        require(!ackTimeout.isNegative) { "api.kafka.coupon-issue.ack-timeout must not be negative" }
        require(topicPartitions > 0) { "api.kafka.coupon-issue.topic-partitions must be greater than 0" }
        require(topicReplicas > 0) { "api.kafka.coupon-issue.topic-replicas must be greater than 0" }
    }
}
