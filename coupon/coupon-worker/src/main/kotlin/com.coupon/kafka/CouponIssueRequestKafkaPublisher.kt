package com.coupon.kafka

import com.coupon.config.CouponIssueRequestKafkaProperties
import com.coupon.coupon.request.CouponIssueRequestedMessage
import com.coupon.support.logging.logger
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(
    prefix = "worker.kafka.coupon-issue-request",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueRequestKafkaPublisher(
    private val couponIssueRequestKafkaProperties: CouponIssueRequestKafkaProperties,
    private val kafkaTemplate: KafkaTemplate<String, CouponIssueRequestedMessage>,
) {
    private val log by logger()

    /**
     * Relay publish waits for broker ack.
     * The request must not move to ENQUEUED before this method returns successfully.
     */
    fun publish(message: CouponIssueRequestedMessage) {
        val record =
            ProducerRecord(
                couponIssueRequestKafkaProperties.topic,
                message.requestId.toString(),
                message,
            )

        val metadata =
            kafkaTemplate
                .send(record)
                .get(couponIssueRequestKafkaProperties.ackTimeout.toMillis(), TimeUnit.MILLISECONDS)

        log.info {
            "Published coupon issue request topic=${metadata.recordMetadata.topic()}, " +
                "partition=${metadata.recordMetadata.partition()}, offset=${metadata.recordMetadata.offset()}, " +
                "requestId=${message.requestId}, couponId=${message.couponId}, userId=${message.userId}"
        }
    }
}
