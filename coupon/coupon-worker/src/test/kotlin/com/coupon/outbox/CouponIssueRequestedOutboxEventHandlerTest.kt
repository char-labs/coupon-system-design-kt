package com.coupon.outbox

import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.kafka.CouponIssueRequestKafkaMetrics
import com.coupon.kafka.CouponIssueRequestKafkaPublisher
import com.coupon.support.outbox.OutboxEvent
import com.coupon.support.outbox.OutboxEventStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CouponIssueRequestedOutboxEventHandlerTest :
    BehaviorSpec({
        given("CouponIssueRequestedOutboxEventHandler가 이벤트를 처리하면") {
            `when`("aggregateId가 유효하지 않으면") {
                val context = CouponIssueRequestedOutboxEventHandlerTestContext()

                val result = context.handler.handle(context.event.copy(aggregateId = "invalid"))

                then("DEAD를 반환한다") {
                    result shouldBe OutboxProcessingResult.Dead("Invalid coupon issue request aggregateId: invalid")
                }
            }

            `when`("request 처리 완료를 받으면") {
                val context = CouponIssueRequestedOutboxEventHandlerTestContext()
                val pendingRequest = couponIssueRequest(status = CouponIssueRequestStatus.PENDING)

                every { context.couponIssueRequestService.getById(1L) } returns pendingRequest
                every { context.couponIssueRequestKafkaPublisher.publish(any()) } returns Unit
                every { context.couponIssueRequestService.markEnqueuedAfterRelay(1L) } returns true
                every { context.couponIssueRequestKafkaMetrics.recordRelayPublished() } returns Unit
                every {
                    context.couponIssueRequestKafkaMetrics.recordStatusTransition(
                        CouponIssueRequestStatus.PENDING,
                        CouponIssueRequestStatus.ENQUEUED,
                    )
                } returns Unit

                val result = context.handler.handle(context.event)

                then("outbox 성공으로 처리한다") {
                    result shouldBe OutboxProcessingResult.Success
                }
            }

            `when`("request 처리 재시도를 받으면") {
                val context = CouponIssueRequestedOutboxEventHandlerTestContext()
                val pendingRequest = couponIssueRequest(status = CouponIssueRequestStatus.PENDING)

                every { context.couponIssueRequestService.getById(1L) } returns pendingRequest
                every { context.couponIssueRequestKafkaPublisher.publish(any()) } throws IllegalStateException("retry later")
                every { context.couponIssueRequestKafkaMetrics.recordRelayFailed() } returns Unit

                val result = context.handler.handle(context.event)

                then("outbox retry를 반환한다") {
                    result shouldBe OutboxProcessingResult.Retry("retry later")
                }
            }

            `when`("request가 이미 ENQUEUED면") {
                val context = CouponIssueRequestedOutboxEventHandlerTestContext()
                val enqueuedRequest =
                    couponIssueRequest(
                        status = CouponIssueRequestStatus.ENQUEUED,
                        enqueuedAt = LocalDateTime.of(2026, 4, 7, 9, 1),
                    )

                every { context.couponIssueRequestService.getById(1L) } returns enqueuedRequest
                every { context.couponIssueRequestKafkaMetrics.recordRelayPublished() } returns Unit

                val result = context.handler.handle(context.event)

                then("중복 publish 없이 성공 처리한다") {
                    result shouldBe OutboxProcessingResult.Success
                    verify(exactly = 0) { context.couponIssueRequestKafkaPublisher.publish(any()) }
                }
            }
        }
    })

private class CouponIssueRequestedOutboxEventHandlerTestContext {
    val couponIssueRequestService: CouponIssueRequestService = mockk()
    val couponIssueRequestKafkaPublisher: CouponIssueRequestKafkaPublisher = mockk()
    val couponIssueRequestKafkaMetrics: CouponIssueRequestKafkaMetrics = mockk(relaxed = true)
    val handler =
        CouponIssueRequestedOutboxEventHandler(
            couponIssueRequestService = couponIssueRequestService,
            couponIssueRequestKafkaPublisher = couponIssueRequestKafkaPublisher,
            couponIssueRequestKafkaMetrics = couponIssueRequestKafkaMetrics,
        )
    val event =
        OutboxEvent(
            id = 1L,
            eventType = CouponOutboxEventType.COUPON_ISSUE_REQUESTED,
            aggregateType = "COUPON_ISSUE_REQUEST",
            aggregateId = "1",
            payloadJson = """{"requestId":1}""",
            status = OutboxEventStatus.PROCESSING,
            dedupeKey = "coupon:10:user:100:action:ISSUE",
            availableAt = LocalDateTime.of(2026, 4, 7, 9, 0),
            retryCount = 0,
            lastError = null,
            processedAt = null,
            createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
            updatedAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        )
}

private fun couponIssueRequest(
    status: CouponIssueRequestStatus,
    enqueuedAt: LocalDateTime? = null,
) = com.coupon.coupon.request.CouponIssueRequest(
    id = 1L,
    couponId = 10L,
    userId = 100L,
    idempotencyKey = "coupon:10:user:100:action:ISSUE",
    status = status,
    resultCode = null,
    couponIssueId = if (status == CouponIssueRequestStatus.SUCCEEDED) 1000L else null,
    failureReason = null,
    enqueuedAt = enqueuedAt,
    processingStartedAt = null,
    deliveryAttemptCount = 0,
    lastDeliveryError = null,
    processedAt = null,
    createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
    updatedAt = LocalDateTime.of(2026, 4, 7, 9, 0),
)
