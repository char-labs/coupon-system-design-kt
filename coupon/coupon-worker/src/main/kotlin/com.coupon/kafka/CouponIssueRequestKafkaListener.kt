package com.coupon.kafka

import com.coupon.coupon.request.CouponIssueRequest
import com.coupon.coupon.request.CouponIssueRequestProcessingResult
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.coupon.request.CouponIssueRequestedMessage
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.support.logging.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "worker.kafka.coupon-issue-request",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueRequestKafkaListener(
    private val couponIssueRequestService: CouponIssueRequestService,
    private val couponIssueRequestKafkaMetrics: CouponIssueRequestKafkaMetrics,
) {
    private val log by logger()

    /**
     * Kafka consumer entrypoint for accepted coupon issue requests.
     * The listener delegates business work to the request service and only decides ack, retry, or DLQ hand-off.
     */
    @KafkaListener(
        topics = ["\${worker.kafka.coupon-issue-request.topic}"],
        groupId = "\${worker.kafka.coupon-issue-request.group-id}",
        containerFactory = "couponIssueRequestKafkaListenerContainerFactory",
    )
    fun consume(
        message: CouponIssueRequestedMessage,
        acknowledgment: Acknowledgment,
    ) {
        log.info {
            "Start coupon issue Kafka consumption requestId=${message.requestId}, " +
                "couponId=${message.couponId}, userId=${message.userId}"
        }

        when (val result = couponIssueRequestService.process(message.requestId)) {
            is CouponIssueRequestProcessingResult.Completed -> {
                if (result.transitioned) {
                    recordCompletedTransition(result.request)
                    logCompletion(result.request)
                } else {
                    log.info { "Coupon issue request ${message.requestId} was already settled with status=${result.request.status}" }
                }
                acknowledgment.acknowledge()
            }

            is CouponIssueRequestProcessingResult.Retry -> {
                if (result.transitioned) {
                    couponIssueRequestKafkaMetrics.recordStatusTransition(
                        from = CouponIssueRequestStatus.PROCESSING,
                        to = CouponIssueRequestStatus.ENQUEUED,
                    )
                }
                couponIssueRequestKafkaMetrics.recordConsumerRetry()
                log.warn { "Retry coupon issue request ${message.requestId}: ${result.reason}" }
                throw CouponIssueRequestKafkaRetryableException(message.requestId, result.reason)
            }

            is CouponIssueRequestProcessingResult.Dead -> {
                log.error { "Dead-letter coupon issue request ${message.requestId}: ${result.reason}" }
                throw CouponIssueRequestKafkaDeadLetterException(message.requestId, result.reason)
            }
        }
    }

    /**
     * DLQ is the final automatic recovery stage.
     * Once a message reaches this listener, the request is explicitly converged to DEAD.
     */
    @KafkaListener(
        topics = ["\${worker.kafka.coupon-issue-request.dlq-topic}"],
        groupId = "\${worker.kafka.coupon-issue-request.dlq-group-id}",
        containerFactory = "couponIssueRequestDlqKafkaListenerContainerFactory",
    )
    fun consumeDlq(
        message: CouponIssueRequestedMessage,
        acknowledgment: Acknowledgment,
        @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) exceptionMessage: String?,
    ) {
        val failureReason =
            exceptionMessage
                ?: "Kafka delivery exhausted after retries for request ${message.requestId}"

        couponIssueRequestService.markDeadAfterDeliveryFailure(
            requestId = message.requestId,
            failureReason = failureReason,
        )

        couponIssueRequestKafkaMetrics.recordDlq()
        log.error {
            "Coupon issue request moved to DLQ requestId=${message.requestId}, " +
                "couponId=${message.couponId}, userId=${message.userId}, reason=$failureReason"
        }
        acknowledgment.acknowledge()
    }

    private fun recordCompletedTransition(request: CouponIssueRequest) {
        when (request.status) {
            CouponIssueRequestStatus.SUCCEEDED,
            CouponIssueRequestStatus.FAILED,
            ->
                couponIssueRequestKafkaMetrics.recordStatusTransition(
                    from = CouponIssueRequestStatus.PROCESSING,
                    to = request.status,
                )

            else -> Unit
        }
    }

    private fun logCompletion(request: CouponIssueRequest) {
        when (request.status) {
            CouponIssueRequestStatus.SUCCEEDED ->
                log.info {
                    "Coupon issue request succeeded requestId=${request.id}, " +
                        "couponId=${request.couponId}, userId=${request.userId}, couponIssueId=${request.couponIssueId}"
                }

            CouponIssueRequestStatus.FAILED ->
                log.info {
                    "Coupon issue request completed with business failure requestId=${request.id}, " +
                        "couponId=${request.couponId}, userId=${request.userId}, resultCode=${request.resultCode}, " +
                        "failureReason=${request.failureReason}"
                }

            else -> Unit
        }
    }
}
