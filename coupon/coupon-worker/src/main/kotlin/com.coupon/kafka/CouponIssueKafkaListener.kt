package com.coupon.kafka

import com.coupon.config.CouponIssueKafkaProperties
import com.coupon.coupon.CouponIssueAsyncExecutionResult
import com.coupon.coupon.CouponIssueFacade
import com.coupon.coupon.CouponIssueMessage
import com.coupon.coupon.CouponIssueService
import com.coupon.support.logging.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "worker.kafka.coupon-issue",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueKafkaListener(
    private val couponIssueFacade: CouponIssueFacade,
    private val couponIssueService: CouponIssueService,
    private val couponIssueKafkaProperties: CouponIssueKafkaProperties,
) {
    private val log by logger()

    @KafkaListener(
        topics = ["\${worker.kafka.coupon-issue.topic}"],
        groupId = "\${worker.kafka.coupon-issue.group-id}",
        containerFactory = "couponIssueKafkaListenerContainerFactory",
    )
    fun consume(
        message: CouponIssueMessage,
        acknowledgment: Acknowledgment,
    ) {
        when (val result = couponIssueFacade.execute(message)) {
            is CouponIssueAsyncExecutionResult.Succeeded -> {
                log.info {
                    "Coupon issue succeeded couponId=${message.couponId}, userId=${message.userId}, couponIssueId=${result.couponIssueId}"
                }
                acknowledgment.acknowledge()
            }

            CouponIssueAsyncExecutionResult.AlreadyIssued -> {
                log.info {
                    "Coupon issue ignored as already issued couponId=${message.couponId}, userId=${message.userId}"
                }
                acknowledgment.acknowledge()
            }

            is CouponIssueAsyncExecutionResult.Rejected -> {
                log.info {
                    "Coupon issue rejected couponId=${message.couponId}, userId=${message.userId}, errorType=${result.errorType}"
                }
                acknowledgment.acknowledge()
            }

            is CouponIssueAsyncExecutionResult.Retry -> {
                throw CouponIssueKafkaRetryableException(
                    couponId = message.couponId,
                    userId = message.userId,
                    message = result.reason,
                )
            }
        }
    }

    @KafkaListener(
        topics = ["\${worker.kafka.coupon-issue.dlq-topic}"],
        groupId = "\${worker.kafka.coupon-issue.dlq-group-id}",
        containerFactory = "couponIssueDlqKafkaListenerContainerFactory",
    )
    fun consumeDlq(
        message: CouponIssueMessage,
        acknowledgment: Acknowledgment,
        @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) exceptionMessage: String?,
    ) {
        couponIssueService.release(
            couponId = message.couponId,
            userId = message.userId,
        )

        log.error {
            "Coupon issue moved to DLQ topic=${couponIssueKafkaProperties.dlqTopic}, couponId=${message.couponId}, " +
                "userId=${message.userId}, reason=${exceptionMessage ?: "Kafka delivery exhausted"}"
        }
        acknowledgment.acknowledge()
    }
}
