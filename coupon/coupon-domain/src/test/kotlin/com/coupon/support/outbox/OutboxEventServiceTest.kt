package com.coupon.support.outbox

import com.coupon.coupon.support.DomainServiceTestRuntime
import com.coupon.support.outbox.command.OutboxEventCommand
import com.coupon.support.outbox.criteria.OutboxEventCriteria
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import java.time.LocalDateTime

class OutboxEventServiceTest :
    BehaviorSpec({
        given("OutboxEventService로 이벤트를 발행하면") {
            `when`("단건 publish 명령이 주어지면") {
                val context = OutboxEventServiceTestContext()
                val availableAt = LocalDateTime.of(2026, 4, 6, 10, 0)
                val command =
                    OutboxEventCommand.Publish(
                        eventType = "USER_DELETION_EMAIL_REQUESTED",
                        aggregateType = "USER",
                        aggregateId = "101",
                        payloadJson = """{"userId":101}""",
                        dedupeKey = "user:101:email",
                        availableAt = availableAt,
                    )
                val criteriaSlot = slot<OutboxEventCriteria.Create>()
                val saved = outboxEvent(id = 1L, eventType = command.eventType, availableAt = availableAt)

                every { context.outboxEventRepository.save(capture(criteriaSlot)) } returns saved

                val result = context.outboxEventService.publish(command)

                then("create criteria로 repository에 저장한다") {
                    result shouldBe saved
                    criteriaSlot.captured shouldBe OutboxEventCriteria.Create.of(command)
                    verify(exactly = 1) { context.outboxEventRepository.save(any()) }
                }
            }

            `when`("여러 publish 명령이 주어지면") {
                val context = OutboxEventServiceTestContext()
                val commands =
                    listOf(
                        OutboxEventCommand.Publish(
                            eventType = "USER_DELETION_EMAIL_REQUESTED",
                            aggregateType = "USER",
                            aggregateId = "101",
                            payloadJson = """{"userId":101}""",
                        ),
                        OutboxEventCommand.Publish(
                            eventType = "USER_DELETION_CACHE_INVALIDATION_REQUESTED",
                            aggregateType = "USER",
                            aggregateId = "101",
                            payloadJson = """{"userId":101}""",
                        ),
                    )
                val saved =
                    listOf(
                        outboxEvent(id = 1L, eventType = commands[0].eventType),
                        outboxEvent(id = 2L, eventType = commands[1].eventType),
                    )

                every { context.outboxEventRepository.saveAll(any()) } returns saved

                val result = context.outboxEventService.publishAll(commands)

                then("batch create criteria로 repository에 저장한다") {
                    result shouldBe saved
                    verify(exactly = 1) {
                        context.outboxEventRepository.saveAll(commands.map(OutboxEventCriteria.Create::of))
                    }
                }
            }
        }

        given("OutboxEventService로 처리 대상을 조회하면") {
            `when`("limit만 주어지면") {
                val context = OutboxEventServiceTestContext()
                val availableAt = LocalDateTime.of(2026, 4, 6, 10, 30)
                val expected =
                    listOf(
                        outboxEvent(id = 11L, status = OutboxEventStatus.PENDING),
                        outboxEvent(id = 12L, status = OutboxEventStatus.FAILED),
                    )

                every {
                    context.outboxEventRepository.findProcessable(
                        statuses = OutboxEventService.PROCESSABLE_STATUSES,
                        availableAt = availableAt,
                        limit = 10,
                    )
                } returns expected

                val result = context.outboxEventService.findProcessable(limit = 10, availableAt = availableAt)

                then("PENDING과 FAILED만 조회 대상으로 사용한다") {
                    result shouldContainExactly expected
                    verifySequence {
                        context.outboxEventRepository.findProcessable(
                            statuses = OutboxEventService.PROCESSABLE_STATUSES,
                            availableAt = availableAt,
                            limit = 10,
                        )
                    }
                }
            }
        }
    })

private class OutboxEventServiceTestContext {
    init {
        DomainServiceTestRuntime.initialize()
    }

    val outboxEventRepository: OutboxEventRepository = mockk()
    val outboxEventService = OutboxEventService(outboxEventRepository)
}

private fun outboxEvent(
    id: Long,
    eventType: String = "USER_DELETION_EMAIL_REQUESTED",
    status: OutboxEventStatus = OutboxEventStatus.PENDING,
    availableAt: LocalDateTime = LocalDateTime.of(2026, 4, 6, 10, 0),
) = OutboxEvent(
    id = id,
    eventType = eventType,
    aggregateType = "USER",
    aggregateId = "101",
    payloadJson = """{"userId":101}""",
    status = status,
    dedupeKey = "user:101:$eventType",
    availableAt = availableAt,
    retryCount = 0,
    lastError = null,
    processedAt = null,
    createdAt = availableAt,
    updatedAt = availableAt,
)
