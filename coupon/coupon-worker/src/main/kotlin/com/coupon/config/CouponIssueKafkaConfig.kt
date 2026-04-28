package com.coupon.config

import com.coupon.coupon.intake.CouponIssueMessage
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
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
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@ConditionalOnProperty(
    prefix = "worker.kafka.coupon-issue",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueKafkaConfig(
    private val couponIssueKafkaProperties: CouponIssueKafkaProperties,
    private val environment: Environment,
) {
    @Bean
    fun couponIssueTopic(): NewTopic =
        TopicBuilder
            .name(couponIssueKafkaProperties.topic)
            .partitions(couponIssueKafkaProperties.topicPartitions)
            .replicas(couponIssueKafkaProperties.topicReplicas)
            .build()

    @Bean
    fun couponIssueDlqTopic(): NewTopic =
        TopicBuilder
            .name(couponIssueKafkaProperties.dlqTopic)
            .partitions(couponIssueKafkaProperties.topicPartitions)
            .replicas(couponIssueKafkaProperties.topicReplicas)
            .build()

    @Bean
    fun couponIssueConsumerFactory(meterRegistry: MeterRegistry): ConsumerFactory<String, CouponIssueMessage> {
        val config =
            linkedMapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JacksonJsonDeserializer::class.java,
                ConsumerConfig.GROUP_ID_CONFIG to couponIssueKafkaProperties.groupId,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG to
                    "org.apache.kafka.clients.consumer.CooperativeStickyAssignor",
            )

        val deserializer =
            JacksonJsonDeserializer(CouponIssueMessage::class.java).apply {
                addTrustedPackages("*")
                setUseTypeHeaders(false)
                setRemoveTypeHeaders(false)
            }

        return DefaultKafkaConsumerFactory(config, StringDeserializer(), deserializer).apply {
            addListener(MicrometerConsumerListener(meterRegistry))
        }
    }

    @Bean
    fun couponIssueProducerFactory(meterRegistry: MeterRegistry): ProducerFactory<String, CouponIssueMessage> {
        val config =
            linkedMapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 1,
            )

        return DefaultKafkaProducerFactory<String, CouponIssueMessage>(config).apply {
            addListener(MicrometerProducerListener(meterRegistry))
        }
    }

    @Bean
    fun couponIssueKafkaTemplate(
        @Qualifier("couponIssueProducerFactory")
        producerFactory: ProducerFactory<String, CouponIssueMessage>,
    ): KafkaTemplate<String, CouponIssueMessage> =
        KafkaTemplate(producerFactory).apply {
            setObservationEnabled(couponIssueKafkaProperties.observationEnabled)
        }

    @Bean
    fun couponIssueDirectErrorHandler(
        @Qualifier("couponIssueKafkaTemplate")
        kafkaTemplate: KafkaTemplate<String, CouponIssueMessage>,
    ): DefaultErrorHandler =
        DefaultErrorHandler(
            DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
                TopicPartition(couponIssueKafkaProperties.dlqTopic, record.partition())
            },
            ExponentialBackOffWithMaxRetries(couponIssueKafkaProperties.retry.maxAttempts - 1).apply {
                initialInterval = couponIssueKafkaProperties.retry.initialInterval.toMillis()
                multiplier = couponIssueKafkaProperties.retry.multiplier
                maxInterval = couponIssueKafkaProperties.retry.maxInterval.toMillis()
            },
        ).apply {
            addNotRetryableExceptions(IllegalArgumentException::class.java)
        }

    @Bean
    fun couponIssueKafkaListenerContainerFactory(
        @Qualifier("couponIssueConsumerFactory")
        consumerFactory: ConsumerFactory<String, CouponIssueMessage>,
        @Qualifier("couponIssueDirectErrorHandler")
        couponIssueDirectErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, CouponIssueMessage> =
        ConcurrentKafkaListenerContainerFactory<String, CouponIssueMessage>().apply {
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(couponIssueDirectErrorHandler)
            setConcurrency(couponIssueKafkaProperties.concurrency)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            containerProperties.isObservationEnabled = couponIssueKafkaProperties.observationEnabled
        }

    @Bean
    fun couponIssueDlqKafkaListenerContainerFactory(
        @Qualifier("couponIssueConsumerFactory")
        consumerFactory: ConsumerFactory<String, CouponIssueMessage>,
    ): ConcurrentKafkaListenerContainerFactory<String, CouponIssueMessage> =
        ConcurrentKafkaListenerContainerFactory<String, CouponIssueMessage>().apply {
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(
                DefaultErrorHandler(
                    FixedBackOff(
                        couponIssueKafkaProperties.dlqRetry.interval.toMillis(),
                        couponIssueKafkaProperties.dlqRetry.maxAttempts - 1,
                    ),
                ),
            )
            setConcurrency(1)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            containerProperties.isObservationEnabled = couponIssueKafkaProperties.observationEnabled
        }

    private fun bootstrapServers(): String = environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092")
}
