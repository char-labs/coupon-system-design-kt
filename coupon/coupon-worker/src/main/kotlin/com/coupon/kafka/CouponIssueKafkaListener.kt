package com.coupon.kafka

import com.coupon.config.CouponIssueKafkaProperties
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.execution.CouponIssueExecutionFacade
import com.coupon.coupon.execution.CouponIssueExecutionResult
import com.coupon.coupon.execution.CouponIssueProcessingLimiter
import com.coupon.coupon.intake.CouponIssueMessage
import com.coupon.shared.logging.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

@Component
@ConditionalOnProperty(
    prefix = "worker.kafka.coupon-issue",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueKafkaListener(
    private val couponIssueExecutionFacade: CouponIssueExecutionFacade,
    private val couponIssueService: CouponIssueService,
    private val couponIssueProcessingLimiter: CouponIssueProcessingLimiter,
    private val couponIssueKafkaProperties: CouponIssueKafkaProperties,
    private val clock: Clock,
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
        val limitStartedAt = System.nanoTime()
        couponIssueProcessingLimiter.acquire()
        val limitDuration = Duration.ofNanos((System.nanoTime() - limitStartedAt).coerceAtLeast(0))
        log.info {
            "event=coupon.issue phase=worker.limit result=ACQUIRED requestId=${message.requestId} " +
                "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                "durationMs=${limitDuration.toMillis()} errorType=NONE"
        }

        when (val result = couponIssueExecutionFacade.execute(message)) {
            is CouponIssueExecutionResult.Succeeded -> {
                val durationMs = Duration.between(message.acceptedAt, clock.instant()).toMillis().coerceAtLeast(0)
                log.info {
                    "event=coupon.issue phase=worker.consume result=SUCCESS requestId=${message.requestId} " +
                        "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                        "errorType=NONE durationMs=$durationMs couponIssueId=${result.couponIssueId}"
                }
                acknowledgment.acknowledge()
            }

            CouponIssueExecutionResult.AlreadyIssued -> {
                val durationMs = Duration.between(message.acceptedAt, clock.instant()).toMillis().coerceAtLeast(0)
                log.info {
                    "event=coupon.issue phase=worker.consume result=ALREADY_ISSUED requestId=${message.requestId} " +
                        "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                        "errorType=ALREADY_ISSUED_COUPON durationMs=$durationMs"
                }
                acknowledgment.acknowledge()
            }

            is CouponIssueExecutionResult.Rejected -> {
                val durationMs = Duration.between(message.acceptedAt, clock.instant()).toMillis().coerceAtLeast(0)
                log.info {
                    "event=coupon.issue phase=worker.consume result=REJECTED requestId=${message.requestId} " +
                        "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                        "errorType=${result.errorType.name} durationMs=$durationMs"
                }
                acknowledgment.acknowledge()
            }

            is CouponIssueExecutionResult.Retry -> {
                val durationMs = Duration.between(message.acceptedAt, clock.instant()).toMillis().coerceAtLeast(0)
                log.warn {
                    "event=coupon.issue phase=worker.consume result=RETRY requestId=${message.requestId} " +
                        "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                        "errorType=RETRYABLE durationMs=$durationMs reason=${result.reason}"
                }
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
        val durationMs = Duration.between(message.acceptedAt, clock.instant()).toMillis().coerceAtLeast(0)

        log.error {
            "event=coupon.issue phase=worker.dlq result=DLQ requestId=${message.requestId} " +
                "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                "errorType=RETRY_EXHAUSTED durationMs=$durationMs topic=${couponIssueKafkaProperties.dlqTopic} " +
                "reason=${exceptionMessage ?: "Kafka delivery exhausted"}"
        }
        acknowledgment.acknowledge()
    }
}
