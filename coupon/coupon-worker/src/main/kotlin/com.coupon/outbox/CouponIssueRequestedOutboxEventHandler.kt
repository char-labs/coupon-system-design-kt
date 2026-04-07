package com.coupon.outbox

import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.coupon.request.CouponIssueRequestedMessage
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.kafka.CouponIssueRequestKafkaMetrics
import com.coupon.kafka.CouponIssueRequestKafkaPublisher
import com.coupon.support.outbox.OutboxEvent
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestedOutboxEventHandler(
    private val couponIssueRequestService: CouponIssueRequestService,
    private val couponIssueRequestKafkaPublisher: CouponIssueRequestKafkaPublisher,
    private val couponIssueRequestKafkaMetrics: CouponIssueRequestKafkaMetrics,
) : OutboxEventHandler {
    override val eventType: String = CouponOutboxEventType.COUPON_ISSUE_REQUESTED

    /**
     * Relay step for accepted coupon issue requests.
     * It never performs the issue itself; it only publishes to Kafka and moves the request from PENDING to ENQUEUED.
     */
    override fun handle(event: OutboxEvent): OutboxProcessingResult {
        val requestId =
            event.aggregateId.toLongOrNull() ?: run {
                couponIssueRequestKafkaMetrics.recordRelayDead()
                return OutboxProcessingResult.Dead("Invalid coupon issue request aggregateId: ${event.aggregateId}")
            }

        val request =
            try {
                couponIssueRequestService.getById(requestId)
            } catch (exception: ErrorException) {
                if (exception.errorType != ErrorType.NOT_FOUND_DATA) {
                    throw exception
                }

                couponIssueRequestKafkaMetrics.recordRelayDead()
                return OutboxProcessingResult.Dead("Coupon issue request $requestId was not found")
            }

        if (request.hasLeftPending()) {
            couponIssueRequestKafkaMetrics.recordRelayPublished()
            return OutboxProcessingResult.Success
        }

        return runCatching {
            couponIssueRequestKafkaPublisher.publish(CouponIssueRequestedMessage.from(request))

            val marked = couponIssueRequestService.markEnqueuedAfterRelay(requestId)

            if (!marked) {
                val current = couponIssueRequestService.getById(requestId)
                if (current.hasLeftPending()) {
                    couponIssueRequestKafkaMetrics.recordRelayPublished()
                    return OutboxProcessingResult.Success
                }

                couponIssueRequestKafkaMetrics.recordRelayFailed()
                return OutboxProcessingResult.Retry(
                    "Coupon issue request $requestId could not be marked ENQUEUED after Kafka publish",
                )
            }

            couponIssueRequestKafkaMetrics.recordRelayPublished()
            couponIssueRequestKafkaMetrics.recordStatusTransition(
                from = CouponIssueRequestStatus.PENDING,
                to = CouponIssueRequestStatus.ENQUEUED,
            )
            OutboxProcessingResult.Success
        }.getOrElse { throwable ->
            couponIssueRequestKafkaMetrics.recordRelayFailed()
            OutboxProcessingResult.Retry(throwable.message ?: "Kafka relay publish failed")
        }
    }
}
