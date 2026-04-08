package com.coupon.config

import com.coupon.coupon.CouponIssueEventPublisher
import com.coupon.coupon.CouponIssueMessage
import com.coupon.coupon.CouponIssuePublishReceipt
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
    override fun publish(message: CouponIssueMessage): CouponIssuePublishReceipt {
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

        return CouponIssuePublishReceipt(
            topic = metadata.topic(),
            partition = metadata.partition(),
            offset = metadata.offset(),
        )
    }
}
