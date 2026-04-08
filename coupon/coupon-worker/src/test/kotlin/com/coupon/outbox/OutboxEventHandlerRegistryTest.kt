package com.coupon.outbox

import com.coupon.shared.outbox.OutboxEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class OutboxEventHandlerRegistryTest :
    BehaviorSpec({
        given("OutboxEventHandlerRegistry를 생성하면") {
            `when`("같은 eventType을 처리하는 핸들러가 둘 이상 있으면") {
                then("중복 등록을 막는다") {
                    shouldThrow<IllegalArgumentException> {
                        OutboxEventHandlerRegistry(
                            listOf(
                                StaticResultHandler("USER_DELETION_EMAIL_REQUESTED"),
                                StaticResultHandler("USER_DELETION_EMAIL_REQUESTED"),
                            ),
                        )
                    }
                }
            }

            `when`("서로 다른 eventType 핸들러가 등록되면") {
                val registry =
                    OutboxEventHandlerRegistry(
                        listOf(
                            StaticResultHandler("USER_DELETION_EMAIL_REQUESTED"),
                            StaticResultHandler("USER_DELETION_CACHE_INVALIDATION_REQUESTED"),
                        ),
                    )

                then("eventType으로 핸들러를 조회할 수 있다") {
                    registry.size() shouldBe 2
                    registry.find("USER_DELETION_EMAIL_REQUESTED")?.eventType shouldBe "USER_DELETION_EMAIL_REQUESTED"
                    registry.find("UNKNOWN_EVENT") shouldBe null
                }
            }
        }
    })

class StaticResultHandler(
    override val eventType: String,
    private val result: OutboxProcessingResult = OutboxProcessingResult.Success,
) : OutboxEventHandler {
    override fun handle(event: OutboxEvent): OutboxProcessingResult = result
}
