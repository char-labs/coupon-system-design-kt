package com.coupon.outbox

import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.request.CouponIssueRequestProcessingResult
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.support.outbox.OutboxEvent
import com.coupon.support.outbox.OutboxEventStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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

                every { context.couponIssueRequestService.process(1L) } returns
                    CouponIssueRequestProcessingResult.Completed(
                        couponIssueRequest(status = CouponIssueRequestStatus.SUCCEEDED),
                    )

                val result = context.handler.handle(context.event)

                then("outbox 성공으로 처리한다") {
                    result shouldBe OutboxProcessingResult.Success
                }
            }

            `when`("request 처리 재시도를 받으면") {
                val context = CouponIssueRequestedOutboxEventHandlerTestContext()

                every { context.couponIssueRequestService.process(1L) } returns
                    CouponIssueRequestProcessingResult.Retry("retry later")

                val result = context.handler.handle(context.event)

                then("outbox retry를 반환한다") {
                    result shouldBe OutboxProcessingResult.Retry("retry later")
                }
            }
        }
    })

private class CouponIssueRequestedOutboxEventHandlerTestContext {
    val couponIssueRequestService: CouponIssueRequestService = mockk()
    val handler = CouponIssueRequestedOutboxEventHandler(couponIssueRequestService)
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

private fun couponIssueRequest(status: CouponIssueRequestStatus) =
    com.coupon.coupon.request.CouponIssueRequest(
        id = 1L,
        couponId = 10L,
        userId = 100L,
        idempotencyKey = "coupon:10:user:100:action:ISSUE",
        status = status,
        resultCode = null,
        couponIssueId = if (status == CouponIssueRequestStatus.SUCCEEDED) 1000L else null,
        failureReason = null,
        processedAt = null,
        createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        updatedAt = LocalDateTime.of(2026, 4, 7, 9, 0),
    )
