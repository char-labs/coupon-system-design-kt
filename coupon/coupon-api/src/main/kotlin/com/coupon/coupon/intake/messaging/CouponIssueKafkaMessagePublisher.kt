package com.coupon.coupon.intake.messaging

import com.coupon.config.CouponIssueKafkaProperties
import com.coupon.coupon.intake.CouponIssueMessage
import com.coupon.coupon.intake.CouponIssueMessagePublisher
import com.coupon.coupon.intake.CouponIssuePublishReceipt
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
class CouponIssueKafkaMessagePublisher(
    private val couponIssueKafkaProperties: CouponIssueKafkaProperties,
    private val kafkaTemplate: KafkaTemplate<String, CouponIssueMessage>,
) : CouponIssueMessagePublisher {
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
