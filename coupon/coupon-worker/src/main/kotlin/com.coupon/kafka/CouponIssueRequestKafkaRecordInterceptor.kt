package com.coupon.kafka

import com.coupon.coupon.request.CouponIssueRequestedMessage
import com.coupon.support.logging.logger
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.listener.RecordInterceptor
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "worker.kafka.coupon-issue-request",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueRequestKafkaRecordInterceptor : RecordInterceptor<String, CouponIssueRequestedMessage> {
    private val log by logger()

    override fun intercept(
        record: ConsumerRecord<String, CouponIssueRequestedMessage>,
        consumer: Consumer<String, CouponIssueRequestedMessage>,
    ): ConsumerRecord<String, CouponIssueRequestedMessage> {
        val message = record.value()
        log.debug {
            "Consuming coupon issue message topic=${record.topic()}, partition=${record.partition()}, " +
                "offset=${record.offset()}, requestId=${message.requestId}, couponId=${message.couponId}, userId=${message.userId}"
        }
        return record
    }

    override fun failure(
        record: ConsumerRecord<String, CouponIssueRequestedMessage>,
        exception: Exception,
        consumer: Consumer<String, CouponIssueRequestedMessage>,
    ) {
        val message = record.value()
        log.warn(exception) {
            "Coupon issue Kafka listener failed topic=${record.topic()}, partition=${record.partition()}, " +
                "offset=${record.offset()}, requestId=${message.requestId}"
        }
    }
}
