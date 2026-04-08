package com.coupon.kafka

import com.coupon.config.CouponIssueKafkaProperties
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.execution.CouponIssueExecutionFacade
import com.coupon.coupon.execution.CouponIssueExecutionResult
import com.coupon.coupon.execution.CouponIssueProcessingLimiter
import com.coupon.coupon.intake.CouponIssueMessage
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import org.springframework.kafka.support.Acknowledgment
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CouponIssueKafkaListenerTest :
    BehaviorSpec({
        given("CouponIssueKafkaListener consume 처리 시") {
            `when`("rate limiter permit을 얻고 발급이 성공하면") {
                val couponIssueExecutionFacade = mockk<CouponIssueExecutionFacade>()
                val couponIssueService = mockk<CouponIssueService>()
                val couponIssueProcessingLimiter = mockk<CouponIssueProcessingLimiter>()
                val acknowledgment = mockk<Acknowledgment>()
                val clock = Clock.fixed(Instant.parse("2026-04-08T00:00:05Z"), ZoneOffset.UTC)
                val properties =
                    CouponIssueKafkaProperties(
                        topic = "coupon.issue.v1",
                        dlqTopic = "coupon.issue.v1.dlq",
                    )
                val listener =
                    CouponIssueKafkaListener(
                        couponIssueExecutionFacade = couponIssueExecutionFacade,
                        couponIssueService = couponIssueService,
                        couponIssueProcessingLimiter = couponIssueProcessingLimiter,
                        couponIssueKafkaProperties = properties,
                        clock = clock,
                    )
                val message =
                    CouponIssueMessage(
                        couponId = 10L,
                        userId = 100L,
                        requestId = "request-1",
                        acceptedAt = Instant.parse("2026-04-08T00:00:00Z"),
                    )

                justRun { couponIssueProcessingLimiter.acquire() }
                every { couponIssueExecutionFacade.execute(message) } returns CouponIssueExecutionResult.Succeeded(1L)
                justRun { acknowledgment.acknowledge() }

                listener.consume(
                    message = message,
                    acknowledgment = acknowledgment,
                )

                then("Redis limiter를 거친 뒤 worker execute와 ack를 수행한다") {
                    verifySequence {
                        couponIssueProcessingLimiter.acquire()
                        couponIssueExecutionFacade.execute(message)
                        acknowledgment.acknowledge()
                    }
                }
            }
        }

        given("CouponIssueKafkaListener DLQ 처리 시") {
            `when`("retry가 소진된 메시지를 받으면") {
                val couponIssueExecutionFacade = mockk<CouponIssueExecutionFacade>()
                val couponIssueService = mockk<CouponIssueService>()
                val couponIssueProcessingLimiter = mockk<CouponIssueProcessingLimiter>()
                val acknowledgment = mockk<Acknowledgment>()
                val clock = Clock.fixed(Instant.parse("2026-04-08T00:00:05Z"), ZoneOffset.UTC)
                val properties =
                    CouponIssueKafkaProperties(
                        topic = "coupon.issue.v1",
                        dlqTopic = "coupon.issue.v1.dlq",
                    )
                val listener =
                    CouponIssueKafkaListener(
                        couponIssueExecutionFacade = couponIssueExecutionFacade,
                        couponIssueService = couponIssueService,
                        couponIssueProcessingLimiter = couponIssueProcessingLimiter,
                        couponIssueKafkaProperties = properties,
                        clock = clock,
                    )
                val message =
                    CouponIssueMessage(
                        couponId = 10L,
                        userId = 100L,
                        requestId = "request-1",
                        acceptedAt = Instant.parse("2026-04-08T00:00:00Z"),
                    )

                io.mockk.justRun { couponIssueService.release(message.couponId, message.userId) }
                io.mockk.justRun { acknowledgment.acknowledge() }

                listener.consumeDlq(
                    message = message,
                    acknowledgment = acknowledgment,
                    exceptionMessage = "retry exhausted",
                )

                then("Redis reserve를 해제한 뒤 ack 한다") {
                    verifySequence {
                        couponIssueService.release(message.couponId, message.userId)
                        acknowledgment.acknowledge()
                    }
                }
            }
        }
    })
