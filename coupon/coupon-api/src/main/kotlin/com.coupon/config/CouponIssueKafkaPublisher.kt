package com.coupon.config

import com.coupon.coupon.CouponIssueEventPublisher
import com.coupon.coupon.CouponIssueMessage
import com.coupon.support.logging.logger
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(
    prefix = "api.kafka.coupon-issue",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueKafkaPublisher(
    private val couponIssueKafkaProperties: CouponIssueKafkaProperties,
    private val kafkaTemplate: KafkaTemplate<String, CouponIssueMessage>,
) : CouponIssueEventPublisher {
    private val log by logger()

    override fun publish(message: CouponIssueMessage) {
        val record =
            ProducerRecord(
                couponIssueKafkaProperties.topic,
                message.couponId.toString(),
                message,
            )

        val metadata =
            kafkaTemplate
                .send(record)
                .get(couponIssueKafkaProperties.ackTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .recordMetadata

        log.info {
            "Published coupon issue message topic=${metadata.topic()}, partition=${metadata.partition()}, " +
                "offset=${metadata.offset()}, couponId=${message.couponId}, userId=${message.userId}"
        }
    }
}
