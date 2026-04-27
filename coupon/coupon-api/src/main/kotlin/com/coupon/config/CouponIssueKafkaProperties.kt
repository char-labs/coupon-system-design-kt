package com.coupon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "api.kafka.coupon-issue")
data class CouponIssueKafkaProperties(
    val enabled: Boolean = true,
    val topic: String = "coupon.issue.v1",
    val ackTimeout: Duration = Duration.ofSeconds(10),
    val observationEnabled: Boolean = true,
    val topicPartitions: Int = 3,
    val topicReplicas: Int = 1,
    val producerMaxInFlightRequestsPerConnection: Int = 5,
    val producerLinger: Duration = Duration.ofMillis(5),
    val producerBatchSizeBytes: Int = 65_536,
    val producerCompressionType: String = "lz4",
    val producerRequestTimeout: Duration = Duration.ofSeconds(5),
    val producerDeliveryTimeout: Duration = Duration.ofSeconds(9),
    val producerMaxBlock: Duration = Duration.ofSeconds(1),
    val producerBufferMemoryBytes: Long = 33_554_432,
) {
    init {
        require(topic.isNotBlank()) { "api.kafka.coupon-issue.topic must not be blank" }
        require(!ackTimeout.isNegative) { "api.kafka.coupon-issue.ack-timeout must not be negative" }
        require(topicPartitions > 0) { "api.kafka.coupon-issue.topic-partitions must be greater than 0" }
        require(topicReplicas > 0) { "api.kafka.coupon-issue.topic-replicas must be greater than 0" }
        require(producerMaxInFlightRequestsPerConnection in 1..5) {
            "api.kafka.coupon-issue.producer-max-in-flight-requests-per-connection must be between 1 and 5"
        }
        require(!producerLinger.isNegative) { "api.kafka.coupon-issue.producer-linger must not be negative" }
        require(producerBatchSizeBytes > 0) { "api.kafka.coupon-issue.producer-batch-size-bytes must be greater than 0" }
        require(producerCompressionType.isNotBlank()) { "api.kafka.coupon-issue.producer-compression-type must not be blank" }
        require(!producerRequestTimeout.isNegative && !producerRequestTimeout.isZero) {
            "api.kafka.coupon-issue.producer-request-timeout must be positive"
        }
        require(!producerDeliveryTimeout.isNegative && !producerDeliveryTimeout.isZero) {
            "api.kafka.coupon-issue.producer-delivery-timeout must be positive"
        }
        require(producerDeliveryTimeout <= ackTimeout) {
            "api.kafka.coupon-issue.producer-delivery-timeout must be less than or equal to ack-timeout"
        }
        require(producerDeliveryTimeout >= producerRequestTimeout.plus(producerLinger)) {
            "api.kafka.coupon-issue.producer-delivery-timeout must be greater than or equal to request-timeout + linger"
        }
        require(!producerMaxBlock.isNegative && !producerMaxBlock.isZero) {
            "api.kafka.coupon-issue.producer-max-block must be positive"
        }
        require(producerBufferMemoryBytes > 0) {
            "api.kafka.coupon-issue.producer-buffer-memory-bytes must be greater than 0"
        }
    }
}
