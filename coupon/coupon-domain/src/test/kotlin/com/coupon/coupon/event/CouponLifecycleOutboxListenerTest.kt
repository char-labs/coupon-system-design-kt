package com.coupon.coupon.event

import com.coupon.coupon.support.DomainServiceTestRuntime
import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.outbox.command.OutboxEventCommand
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

class CouponLifecycleOutboxListenerTest :
    BehaviorSpec({
        given("CouponLifecycleOutboxListener가 라이프사이클 이벤트를 받으면") {
            `when`("Issued 이벤트가 오면") {
                val context = CouponLifecycleOutboxListenerTestContext()
                val occurredAt = LocalDateTime.of(2026, 4, 7, 10, 0)
                val event =
                    CouponLifecycleDomainEvent.Issued(
                        couponIssueId = 101L,
                        couponId = 11L,
                        userId = 7L,
                        occurredAt = occurredAt,
                    )
                val commandSlot = slot<OutboxEventCommand.Publish>()

                every { context.outboxEventService.publish(capture(commandSlot)) } returns mockk()

                context.listener.handleIssued(event)

                then("COUPON_ISSUED outbox row를 저장한다") {
                    commandSlot.captured.eventType shouldBe CouponOutboxEventType.COUPON_ISSUED
                    commandSlot.captured.aggregateType shouldBe "COUPON_ISSUE"
                    commandSlot.captured.aggregateId shouldBe "101"
                    commandSlot.captured.dedupeKey shouldBe "coupon-activity:101:COUPON_ISSUED"
                    commandSlot.captured.availableAt shouldBe occurredAt
                    commandSlot.captured.payloadJson shouldBe
                        context.objectMapper.writeValueAsString(
                            CouponLifecycleOutboxPayload(
                                couponIssueId = 101L,
                                couponId = 11L,
                                userId = 7L,
                                occurredAt = occurredAt,
                            ),
                        )
                }
            }
        }
    })

private class CouponLifecycleOutboxListenerTestContext {
    init {
        DomainServiceTestRuntime.initialize()
    }

    val outboxEventService: OutboxEventService = mockk()
    val objectMapper = jacksonObjectMapper()
    val listener = CouponLifecycleOutboxListener(outboxEventService, objectMapper)
}
