package com.coupon.config

import com.coupon.coupon.request.CouponIssueRequestedMessage
import com.coupon.kafka.CouponIssueRequestKafkaDeadLetterException
import com.coupon.kafka.CouponIssueRequestKafkaMetrics
import com.coupon.kafka.CouponIssueRequestKafkaPayloadException
import com.coupon.kafka.CouponIssueRequestKafkaRecordInterceptor
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.core.MicrometerProducerListener
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@ConditionalOnProperty(
    prefix = "worker.kafka.coupon-issue-request",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueRequestKafkaConfig(
    private val couponIssueRequestKafkaProperties: CouponIssueRequestKafkaProperties,
    private val environment: Environment,
) {
    @Bean
    fun couponIssueRequestedTopic(): NewTopic =
        TopicBuilder
            .name(couponIssueRequestKafkaProperties.topic)
            .partitions(couponIssueRequestKafkaProperties.topicPartitions)
            .replicas(couponIssueRequestKafkaProperties.topicReplicas)
            .build()

    @Bean
    fun couponIssueRequestedDlqTopic(): NewTopic =
        TopicBuilder
            .name(couponIssueRequestKafkaProperties.dlqTopic)
            .partitions(couponIssueRequestKafkaProperties.topicPartitions)
            .replicas(couponIssueRequestKafkaProperties.topicReplicas)
            .build()

    @Bean
    fun couponIssueRequestConsumerFactory(meterRegistry: MeterRegistry): ConsumerFactory<String, CouponIssueRequestedMessage> {
        val config =
            linkedMapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JacksonJsonDeserializer::class.java,
                ConsumerConfig.GROUP_ID_CONFIG to couponIssueRequestKafkaProperties.groupId,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to couponIssueRequestKafkaProperties.consumer.autoOffsetReset,
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to
                    couponIssueRequestKafkaProperties.consumer.sessionTimeout
                        .toMillis()
                        .toInt(),
                ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to
                    couponIssueRequestKafkaProperties.consumer.heartbeatInterval
                        .toMillis()
                        .toInt(),
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to
                    couponIssueRequestKafkaProperties.consumer.maxPollInterval
                        .toMillis()
                        .toInt(),
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to couponIssueRequestKafkaProperties.consumer.maxPollRecords,
                ConsumerConfig.FETCH_MIN_BYTES_CONFIG to couponIssueRequestKafkaProperties.consumer.fetchMinBytes,
                ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to
                    couponIssueRequestKafkaProperties.consumer.fetchMaxWait
                        .toMillis()
                        .toInt(),
                ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG to
                    couponIssueRequestKafkaProperties.consumer.partitionAssignmentStrategy,
            )

        val deserializer =
            JacksonJsonDeserializer(CouponIssueRequestedMessage::class.java).apply {
                addTrustedPackages("*")
                setUseTypeHeaders(false)
                setRemoveTypeHeaders(false)
            }

        return DefaultKafkaConsumerFactory(config, StringDeserializer(), deserializer).apply {
            addListener(MicrometerConsumerListener(meterRegistry))
        }
    }

    @Bean
    fun couponIssueRequestProducerFactory(meterRegistry: MeterRegistry): ProducerFactory<String, CouponIssueRequestedMessage> {
        val config =
            linkedMapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to couponIssueRequestKafkaProperties.producer.acks,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to couponIssueRequestKafkaProperties.producer.enableIdempotence,
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to
                    couponIssueRequestKafkaProperties.producer.maxInFlightRequestsPerConnection,
            )

        return DefaultKafkaProducerFactory<String, CouponIssueRequestedMessage>(config).apply {
            addListener(MicrometerProducerListener(meterRegistry))
        }
    }

    @Bean
    fun couponIssueRequestKafkaTemplate(
        producerFactory: ProducerFactory<String, CouponIssueRequestedMessage>,
    ): KafkaTemplate<String, CouponIssueRequestedMessage> = KafkaTemplate(producerFactory)

    @Bean
    fun couponIssueErrorHandler(
        kafkaTemplate: KafkaTemplate<String, CouponIssueRequestedMessage>,
        couponIssueRequestKafkaMetrics: CouponIssueRequestKafkaMetrics,
    ): DefaultErrorHandler {
        val errorHandler =
            DefaultErrorHandler(
                DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
                    TopicPartition(couponIssueRequestKafkaProperties.dlqTopic, record.partition())
                },
                ExponentialBackOffWithMaxRetries(couponIssueRequestKafkaProperties.retry.maxAttempts - 1).apply {
                    initialInterval = couponIssueRequestKafkaProperties.retry.initialInterval.toMillis()
                    multiplier = couponIssueRequestKafkaProperties.retry.multiplier
                    maxInterval = couponIssueRequestKafkaProperties.retry.maxInterval.toMillis()
                },
            )

        errorHandler.addNotRetryableExceptions(
            CouponIssueRequestKafkaPayloadException::class.java,
            CouponIssueRequestKafkaDeadLetterException::class.java,
        )
        errorHandler.setRetryListeners(
            RetryListener { _, throwable, deliveryAttempt ->
                couponIssueRequestKafkaMetrics.recordErrorHandlerRetry(deliveryAttempt, throwable)
            },
        )
        return errorHandler
    }

    @Bean
    fun couponIssueRequestKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, CouponIssueRequestedMessage>,
        couponIssueRequestKafkaRecordInterceptor: CouponIssueRequestKafkaRecordInterceptor,
        couponIssueErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequestedMessage> =
        ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequestedMessage>().apply {
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(couponIssueErrorHandler)
            setRecordInterceptor(couponIssueRequestKafkaRecordInterceptor)
            setConcurrency(couponIssueRequestKafkaProperties.concurrency)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            containerProperties.isObservationEnabled = couponIssueRequestKafkaProperties.observationEnabled
        }

    @Bean
    fun couponIssueRequestDlqKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, CouponIssueRequestedMessage>,
    ): ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequestedMessage> =
        ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequestedMessage>().apply {
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(DefaultErrorHandler(FixedBackOff(0L, 0L)))
            setConcurrency(1)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            containerProperties.isObservationEnabled = couponIssueRequestKafkaProperties.observationEnabled
        }

    private fun bootstrapServers(): String = environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092")
}
