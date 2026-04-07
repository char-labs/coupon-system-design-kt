package com.coupon.outbox

import com.coupon.config.OutboxWorkerProperties
import com.coupon.support.outbox.OutboxEvent
import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.outbox.OutboxEventStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class OutboxDispatcherTest :
    BehaviorSpec({
        given("OutboxDispatcher가 이벤트를 처리하면") {
            `when`("핸들러가 성공을 반환하면") {
                val context =
                    OutboxDispatcherTestContext(
                        handlers = listOf(StaticResultHandler("USER_DELETION_EMAIL_REQUESTED")),
                    )

                every {
                    context.outboxEventService.markSucceeded(
                        eventId = 1L,
                        processedAt = context.now,
                    )
                } returns true

                context.outboxDispatcher.dispatch(context.event)

                then("SUCCEEDED로 마킹한다") {
                    verify(exactly = 1) {
                        context.outboxEventService.markSucceeded(
                            eventId = 1L,
                            processedAt = context.now,
                        )
                    }
                    verify(exactly = 0) { context.outboxEventService.reschedule(any(), any(), any(), any()) }
                    verify(exactly = 0) { context.outboxEventService.markDead(any(), any(), any()) }
                }
            }

            `when`("등록된 핸들러가 없으면") {
                val context = OutboxDispatcherTestContext()
                val deadReason = slot<String>()

                every {
                    context.outboxEventService.markDead(
                        eventId = 1L,
                        lastError = capture(deadReason),
                        processedAt = context.now,
                    )
                } returns true

                context.outboxDispatcher.dispatch(context.event)

                then("즉시 DEAD로 마킹한다") {
                    deadReason.captured shouldBe "No handler registered for outbox event type USER_DELETION_EMAIL_REQUESTED"
                    verify(exactly = 0) { context.outboxEventService.reschedule(any(), any(), any(), any()) }
                }
            }

            `when`("핸들러 실행이 예외로 실패하고 재시도 여유가 남아 있으면") {
                val handler = ThrowingHandler("USER_DELETION_EMAIL_REQUESTED")
                val context = OutboxDispatcherTestContext(handlers = listOf(handler))
                val lastError = slot<String>()

                every {
                    context.outboxEventService.reschedule(
                        eventId = 1L,
                        availableAt = context.now.plusSeconds(1),
                        retryCount = 1,
                        lastError = capture(lastError),
                    )
                } returns true

                context.outboxDispatcher.dispatch(context.event)

                then("retry 정책에 따라 재예약한다") {
                    lastError.captured shouldBe "IllegalStateException: boom"
                    verify(exactly = 1) {
                        context.outboxEventService.reschedule(
                            eventId = 1L,
                            availableAt = context.now.plusSeconds(1),
                            retryCount = 1,
                            lastError = any(),
                        )
                    }
                    verify(exactly = 0) { context.outboxEventService.markDead(any(), any(), any()) }
                }
            }

            `when`("핸들러 실행이 예외로 실패했고 최대 재시도 횟수를 넘기면") {
                val handler = ThrowingHandler("USER_DELETION_EMAIL_REQUESTED")
                val context =
                    OutboxDispatcherTestContext(
                        handlers = listOf(handler),
                        maxRetries = 1,
                        event = outboxEvent(retryCount = 1),
                    )
                val deadReason = slot<String>()

                every {
                    context.outboxEventService.markDead(
                        eventId = 1L,
                        lastError = capture(deadReason),
                        processedAt = context.now,
                    )
                } returns true

                context.outboxDispatcher.dispatch(context.event)

                then("DEAD로 마킹하고 더 이상 재예약하지 않는다") {
                    deadReason.captured shouldBe "IllegalStateException: boom"
                    verify(exactly = 1) {
                        context.outboxEventService.markDead(
                            eventId = 1L,
                            lastError = any(),
                            processedAt = context.now,
                        )
                    }
                    verify(exactly = 0) { context.outboxEventService.reschedule(any(), any(), any(), any()) }
                }
            }
        }
    })

private class OutboxDispatcherTestContext(
    handlers: List<OutboxEventHandler> = emptyList(),
    maxRetries: Int = 10,
    val event: OutboxEvent = outboxEvent(),
) {
    private val clock: Clock =
        Clock.fixed(
            Instant.parse("2026-04-07T00:00:00Z"),
            ZoneId.of("Asia/Seoul"),
        )

    val now: LocalDateTime = LocalDateTime.ofInstant(clock.instant(), clock.zone)
    val outboxEventService: OutboxEventService = mockk()
    private val outboxEventHandlerRegistry = OutboxEventHandlerRegistry(handlers)
    private val outboxWorkerMetrics = OutboxWorkerMetrics(SimpleMeterRegistry(), outboxEventHandlerRegistry)
    private val outboxRetryPolicy =
        OutboxRetryPolicy(
            outboxWorkerProperties =
                OutboxWorkerProperties(
                    maxRetries = maxRetries,
                    retry =
                        OutboxWorkerProperties.Retry(
                            initialDelay = Duration.ofSeconds(1),
                            maxDelay = Duration.ofMinutes(5),
                            multiplier = 2.0,
                        ),
                ),
            clock = clock,
        )
    val outboxDispatcher =
        OutboxDispatcher(
            outboxEventService = outboxEventService,
            outboxEventHandlerRegistry = outboxEventHandlerRegistry,
            outboxRetryPolicy = outboxRetryPolicy,
            outboxWorkerMetrics = outboxWorkerMetrics,
            clock = clock,
        )
}

private class ThrowingHandler(
    override val eventType: String,
) : OutboxEventHandler {
    override fun handle(event: OutboxEvent): OutboxProcessingResult = throw IllegalStateException("boom")
}

private fun outboxEvent(retryCount: Int = 0) =
    OutboxEvent(
        id = 1L,
        eventType = "USER_DELETION_EMAIL_REQUESTED",
        aggregateType = "USER",
        aggregateId = "101",
        payloadJson = """{"userId":101}""",
        status = OutboxEventStatus.PROCESSING,
        dedupeKey = "user:101:USER_DELETION_EMAIL_REQUESTED",
        availableAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        retryCount = retryCount,
        lastError = null,
        processedAt = null,
        createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
        updatedAt = LocalDateTime.of(2026, 4, 7, 9, 0),
    )
