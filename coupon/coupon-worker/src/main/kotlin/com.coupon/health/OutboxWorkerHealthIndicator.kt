package com.coupon.health

import com.coupon.config.CouponIssueRequestKafkaProperties
import com.coupon.config.CouponIssueRequestReconciliationProperties
import com.coupon.config.OutboxWorkerProperties
import com.coupon.outbox.OutboxEventHandlerRegistry
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component("outboxWorker")
class OutboxWorkerHealthIndicator(
    private val outboxWorkerProperties: OutboxWorkerProperties,
    private val couponIssueRequestReconciliationProperties: CouponIssueRequestReconciliationProperties,
    private val couponIssueRequestKafkaProperties: CouponIssueRequestKafkaProperties,
    private val environment: Environment,
    private val outboxEventHandlerRegistry: OutboxEventHandlerRegistry,
) : HealthIndicator {
    override fun health(): Health =
        Health
            .up()
            .withDetail("enabled", outboxWorkerProperties.enabled)
            .withDetail("batchSize", outboxWorkerProperties.batchSize)
            .withDetail("maxRetries", outboxWorkerProperties.maxRetries)
            .withDetail("requestReconciliationEnabled", couponIssueRequestReconciliationProperties.enabled)
            .withDetail("requestReconciliationBatchSize", couponIssueRequestReconciliationProperties.batchSize)
            .withDetail("requestReconciliationPendingTimeout", couponIssueRequestReconciliationProperties.pendingTimeout)
            .withDetail("requestReconciliationProcessingTimeout", couponIssueRequestReconciliationProperties.processingTimeout)
            .withDetail("couponIssueKafkaEnabled", couponIssueRequestKafkaProperties.enabled)
            .withDetail(
                "couponIssueKafkaBootstrapServers",
                environment.getProperty("spring.kafka.bootstrap-servers", ""),
            ).withDetail("couponIssueKafkaTopic", couponIssueRequestKafkaProperties.topic)
            .withDetail("couponIssueKafkaDlqTopic", couponIssueRequestKafkaProperties.dlqTopic)
            .withDetail("couponIssueKafkaGroupId", couponIssueRequestKafkaProperties.groupId)
            .withDetail("couponIssueKafkaDlqGroupId", couponIssueRequestKafkaProperties.dlqGroupId)
            .withDetail("couponIssueKafkaConcurrency", couponIssueRequestKafkaProperties.concurrency)
            .withDetail("couponIssueKafkaObservationEnabled", couponIssueRequestKafkaProperties.observationEnabled)
            .withDetail("couponIssueKafkaMaxPollRecords", couponIssueRequestKafkaProperties.consumer.maxPollRecords)
            .withDetail("couponIssueKafkaMaxPollInterval", couponIssueRequestKafkaProperties.consumer.maxPollInterval)
            .withDetail("couponIssueKafkaFetchMinBytes", couponIssueRequestKafkaProperties.consumer.fetchMinBytes)
            .withDetail("couponIssueKafkaFetchMaxWait", couponIssueRequestKafkaProperties.consumer.fetchMaxWait)
            .withDetail("registeredHandlerCount", outboxEventHandlerRegistry.size())
            .withDetail("registeredEventTypes", outboxEventHandlerRegistry.eventTypes())
            .build()
}
