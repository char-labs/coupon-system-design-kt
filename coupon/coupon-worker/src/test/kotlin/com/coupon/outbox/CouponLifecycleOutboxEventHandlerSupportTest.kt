package com.coupon.outbox

import com.coupon.coupon.activity.CouponActivity
import com.coupon.coupon.activity.CouponActivityService
import com.coupon.coupon.activity.criteria.CouponActivityCriteria
import com.coupon.enums.coupon.CouponActivityType
import com.coupon.shared.outbox.OutboxEvent
import com.coupon.shared.outbox.OutboxEventStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

class CouponLifecycleOutboxEventHandlerSupportTest :
    BehaviorSpec({
        given("CouponLifecycleOutboxEventHandlerSupport가 payload를 처리하면") {
            `when`("payload가 정상이면") {
                val context = CouponLifecycleOutboxEventHandlerSupportTestContext()
                val criteriaSlot = slot<CouponActivityCriteria.Create>()

                every { context.couponActivityService.recordIfAbsent(capture(criteriaSlot)) } returns
                    couponActivity(activityType = CouponActivityType.USED)

                val result = context.support.handle(context.event, CouponActivityType.USED)

                then("activity projection을 기록하고 성공을 반환한다") {
                    result shouldBe OutboxProcessingResult.Success
                    criteriaSlot.captured shouldBe
                        CouponActivityCriteria.Create(
                            couponIssueId = 1L,
                            couponId = 10L,
                            userId = 100L,
                            activityType = CouponActivityType.USED,
                            occurredAt = LocalDateTime.of(2026, 4, 7, 9, 0),
                        )
                }
            }

            `when`("payload가 잘못되면") {
                val context =
                    CouponLifecycleOutboxEventHandlerSupportTestContext(
                        event = outboxEvent(payloadJson = """{"couponIssueId":"broken"}"""),
                    )

                val result = context.support.handle(context.event, CouponActivityType.CANCELED)

                then("DEAD를 반환한다") {
                    (result is OutboxProcessingResult.Dead) shouldBe true
                }
            }
        }
    })

private class CouponLifecycleOutboxEventHandlerSupportTestContext(
    val event: OutboxEvent = outboxEvent(),
) {
    val couponActivityService: CouponActivityService = mockk()
    val support =
        CouponLifecycleOutboxEventHandlerSupport(
            couponActivityService = couponActivityService,
            objectMapper = jacksonObjectMapper(),
        )
}

private fun outboxEvent(payloadJson: String = payload()) =
    OutboxEvent(
        id = 1L,
        eventType = "COUPON_USED",
        aggregateType = "COUPON_ISSUE",
        aggregateId = "1",
        payloadJson = payloadJson,
        status = OutboxEventStatus.PROCESSING,
        dedupeKey = "coupon-activity:1:COUPON_USED",
        availableAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        retryCount = 0,
        lastError = null,
        processedAt = null,
        createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        updatedAt = LocalDateTime.of(2026, 4, 7, 9, 0),
    )

private fun payload(): String = """{"couponIssueId":1,"couponId":10,"userId":100,"occurredAt":"2026-04-07T09:00:00"}"""

private fun couponActivity(activityType: CouponActivityType) =
    CouponActivity(
        id = 1L,
        couponIssueId = 1L,
        couponId = 10L,
        userId = 100L,
        activityType = activityType,
        occurredAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        createdAt = LocalDateTime.of(2026, 4, 7, 9, 1),
        updatedAt = null,
    )
