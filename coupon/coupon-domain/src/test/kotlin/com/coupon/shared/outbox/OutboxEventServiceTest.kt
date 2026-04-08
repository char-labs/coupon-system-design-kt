package com.coupon.shared.outbox

import com.coupon.shared.outbox.command.OutboxEventCommand
import com.coupon.shared.outbox.criteria.OutboxEventCriteria
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

        given("OutboxEventService로 stale PROCESSING 이벤트를 복구하면") {
            `when`("기준 시각보다 오래된 이벤트가 있으면") {
                val context = OutboxEventServiceTestContext()
                val updatedBefore = LocalDateTime.of(2026, 4, 6, 11, 0)
                val availableAt = LocalDateTime.of(2026, 4, 6, 11, 5)

                every {
                    context.outboxEventRepository.recoverStuckProcessing(
                        updatedBefore = updatedBefore,
                        availableAt = availableAt,
                        lastError = "Recovered stale PROCESSING event",
                    )
                } returns 2

                val result =
                    context.outboxEventService.recoverStuckProcessing(
                        updatedBefore = updatedBefore,
                        availableAt = availableAt,
                    )

                then("FAILED로 되돌린 건수를 반환한다") {
                    result shouldBe 2
                    verify(exactly = 1) {
                        context.outboxEventRepository.recoverStuckProcessing(
                            updatedBefore = updatedBefore,
                            availableAt = availableAt,
                            lastError = "Recovered stale PROCESSING event",
                        )
                    }
                }
            }
        }
    })

private class OutboxEventServiceTestContext {
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
