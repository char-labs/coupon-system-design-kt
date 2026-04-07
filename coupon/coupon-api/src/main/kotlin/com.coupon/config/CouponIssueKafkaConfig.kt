package com.coupon.config

import com.coupon.coupon.CouponIssueMessage
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.MicrometerProducerListener
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
@ConditionalOnProperty(
    prefix = "api.kafka.coupon-issue",
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
    fun couponIssueKafkaTemplate(producerFactory: ProducerFactory<String, CouponIssueMessage>): KafkaTemplate<String, CouponIssueMessage> =
        KafkaTemplate(producerFactory)

    private fun bootstrapServers(): String = environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092")
}
