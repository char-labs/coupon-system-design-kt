package com.coupon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "worker.kafka.coupon-issue-request")
data class CouponIssueRequestKafkaProperties(
    val enabled: Boolean = true,
    val topic: String = "coupon.issue.requested.v1",
    val dlqTopic: String = "coupon.issue.requested.v1.dlq",
    val groupId: String = "coupon-issue-request-group",
    val dlqGroupId: String = "coupon-issue-request-dlq-group",
    val concurrency: Int = 3,
    val observationEnabled: Boolean = true,
    val ackTimeout: Duration = Duration.ofSeconds(10),
    val topicPartitions: Int = 3,
    val topicReplicas: Int = 1,
    val consumer: Consumer = Consumer(),
    val producer: Producer = Producer(),
    val retry: Retry = Retry(),
) {
    init {
        require(topic.isNotBlank()) { "worker.kafka.coupon-issue-request.topic must not be blank" }
        require(dlqTopic.isNotBlank()) { "worker.kafka.coupon-issue-request.dlq-topic must not be blank" }
        require(groupId.isNotBlank()) { "worker.kafka.coupon-issue-request.group-id must not be blank" }
        require(dlqGroupId.isNotBlank()) { "worker.kafka.coupon-issue-request.dlq-group-id must not be blank" }
        require(concurrency > 0) { "worker.kafka.coupon-issue-request.concurrency must be greater than 0" }
        require(!ackTimeout.isNegative) { "worker.kafka.coupon-issue-request.ack-timeout must not be negative" }
        require(topicPartitions > 0) { "worker.kafka.coupon-issue-request.topic-partitions must be greater than 0" }
        require(topicReplicas > 0) { "worker.kafka.coupon-issue-request.topic-replicas must be greater than 0" }
    }

    data class Consumer(
        val autoOffsetReset: String = "earliest",
        val sessionTimeout: Duration = Duration.ofSeconds(30),
        val heartbeatInterval: Duration = Duration.ofSeconds(10),
        val maxPollInterval: Duration = Duration.ofMinutes(10),
        val maxPollRecords: Int = 1000,
        val fetchMinBytes: Int = 1024,
        val fetchMaxWait: Duration = Duration.ofMillis(200),
        val partitionAssignmentStrategy: String = "org.apache.kafka.clients.consumer.CooperativeStickyAssignor",
    ) {
        init {
            require(autoOffsetReset.isNotBlank()) {
                "worker.kafka.coupon-issue-request.consumer.auto-offset-reset must not be blank"
            }
            require(!sessionTimeout.isNegative) {
                "worker.kafka.coupon-issue-request.consumer.session-timeout must not be negative"
            }
            require(!heartbeatInterval.isNegative) {
                "worker.kafka.coupon-issue-request.consumer.heartbeat-interval must not be negative"
            }
            require(!maxPollInterval.isNegative) {
                "worker.kafka.coupon-issue-request.consumer.max-poll-interval must not be negative"
            }
            require(maxPollRecords > 0) {
                "worker.kafka.coupon-issue-request.consumer.max-poll-records must be greater than 0"
            }
            require(fetchMinBytes >= 0) {
                "worker.kafka.coupon-issue-request.consumer.fetch-min-bytes must be 0 or greater"
            }
            require(!fetchMaxWait.isNegative) {
                "worker.kafka.coupon-issue-request.consumer.fetch-max-wait must not be negative"
            }
            require(partitionAssignmentStrategy.isNotBlank()) {
                "worker.kafka.coupon-issue-request.consumer.partition-assignment-strategy must not be blank"
            }
        }
    }

    data class Producer(
        val acks: String = "all",
        val enableIdempotence: Boolean = true,
        val maxInFlightRequestsPerConnection: Int = 1,
    ) {
        init {
            require(acks.isNotBlank()) { "worker.kafka.coupon-issue-request.producer.acks must not be blank" }
            require(maxInFlightRequestsPerConnection > 0) {
                "worker.kafka.coupon-issue-request.producer.max-in-flight-requests-per-connection must be greater than 0"
            }
        }
    }

    data class Retry(
        val maxAttempts: Int = 5,
        val initialInterval: Duration = Duration.ofSeconds(1),
        val multiplier: Double = 2.0,
        val maxInterval: Duration = Duration.ofSeconds(30),
    ) {
        init {
            require(maxAttempts > 0) { "worker.kafka.coupon-issue-request.retry.max-attempts must be greater than 0" }
            require(!initialInterval.isNegative) {
                "worker.kafka.coupon-issue-request.retry.initial-interval must not be negative"
            }
            require(multiplier >= 1.0) { "worker.kafka.coupon-issue-request.retry.multiplier must be at least 1.0" }
            require(!maxInterval.isNegative) {
                "worker.kafka.coupon-issue-request.retry.max-interval must not be negative"
            }
            require(maxInterval >= initialInterval) {
                "worker.kafka.coupon-issue-request.retry.max-interval must be greater than or equal to initial-interval"
            }
        }
    }
}
