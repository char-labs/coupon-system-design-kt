package com.coupon.kafka

import com.coupon.coupon.request.CouponIssueRequest
import com.coupon.coupon.request.CouponIssueRequestProcessingResult
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.coupon.request.CouponIssueRequestedMessage
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime

class CouponIssueRequestKafkaListenerTest :
    BehaviorSpec({
        given("CouponIssueRequestKafkaListener가 메시지를 소비하면") {
            `when`("요청 처리가 성공하면") {
                val context = CouponIssueRequestKafkaListenerTestContext()
                val succeededRequest =
                    couponIssueRequest(
                        status = CouponIssueRequestStatus.SUCCEEDED,
                        couponIssueId = 101L,
                    )
                every { context.couponIssueRequestService.process(1L) } returns
                    CouponIssueRequestProcessingResult.Completed(succeededRequest)

                context.listener.consume(context.message, context.acknowledgment)

                then("ack 하고 SUCCEEDED 전이를 기록한다") {
                    verify { context.acknowledgment.acknowledge() }
                    verify {
                        context.couponIssueRequestKafkaMetrics.recordStatusTransition(
                            CouponIssueRequestStatus.PROCESSING,
                            CouponIssueRequestStatus.SUCCEEDED,
                        )
                    }
                }
            }

            `when`("비즈니스 실패면") {
                val context = CouponIssueRequestKafkaListenerTestContext()
                val failedRequest =
                    couponIssueRequest(
                        status = CouponIssueRequestStatus.FAILED,
                        resultCode = CouponCommandResultCode.ALREADY_ISSUED,
                        failureReason = "이미 발급된 쿠폰입니다.",
                    )
                every { context.couponIssueRequestService.process(1L) } returns
                    CouponIssueRequestProcessingResult.Completed(failedRequest)

                context.listener.consume(context.message, context.acknowledgment)

                then("ack 하고 FAILED 전이를 기록한다") {
                    verify { context.acknowledgment.acknowledge() }
                    verify {
                        context.couponIssueRequestKafkaMetrics.recordStatusTransition(
                            CouponIssueRequestStatus.PROCESSING,
                            CouponIssueRequestStatus.FAILED,
                        )
                    }
                }
            }

            `when`("재시도 가능한 실패면") {
                val context = CouponIssueRequestKafkaListenerTestContext()
                every { context.couponIssueRequestService.process(1L) } returns
                    CouponIssueRequestProcessingResult.Retry(
                        reason = "ErrorException: failed to acquire lock",
                        request = couponIssueRequest(status = CouponIssueRequestStatus.ENQUEUED),
                        transitioned = true,
                    )

                val exception =
                    shouldThrow<CouponIssueRequestKafkaRetryableException> {
                        context.listener.consume(context.message, context.acknowledgment)
                    }

                then("retryable 예외를 던지고 ENQUEUED 복귀를 기록한다") {
                    exception.requestId shouldBe 1L
                    verify(exactly = 0) { context.acknowledgment.acknowledge() }
                    verify { context.couponIssueRequestKafkaMetrics.recordConsumerRetry() }
                    verify {
                        context.couponIssueRequestKafkaMetrics.recordStatusTransition(
                            CouponIssueRequestStatus.PROCESSING,
                            CouponIssueRequestStatus.ENQUEUED,
                        )
                    }
                }
            }

            `when`("dead-letter 대상이면") {
                val context = CouponIssueRequestKafkaListenerTestContext()
                every { context.couponIssueRequestService.process(1L) } returns
                    CouponIssueRequestProcessingResult.Dead("request not found")

                val exception =
                    shouldThrow<CouponIssueRequestKafkaDeadLetterException> {
                        context.listener.consume(context.message, context.acknowledgment)
                    }

                then("DLQ 전송용 예외를 던진다") {
                    exception.requestId shouldBe 1L
                    verify(exactly = 0) { context.acknowledgment.acknowledge() }
                }
            }
        }

        given("CouponIssueRequestKafkaListener가 DLQ를 소비하면") {
            `when`("request를 DEAD로 마킹할 수 있으면") {
                val context = CouponIssueRequestKafkaListenerTestContext()

                every { context.couponIssueRequestService.markDeadAfterDeliveryFailure(1L, "retry exhausted") } returns true

                context.listener.consumeDlq(context.message, context.acknowledgment, "retry exhausted")

                then("dead 상태 전이를 기록하고 ack 한다") {
                    verify { context.acknowledgment.acknowledge() }
                    verify { context.couponIssueRequestKafkaMetrics.recordDlq() }
                }
            }
        }
    })

private class CouponIssueRequestKafkaListenerTestContext {
    val couponIssueRequestService: CouponIssueRequestService = mockk()
    val couponIssueRequestKafkaMetrics: CouponIssueRequestKafkaMetrics = mockk(relaxed = true)
    val acknowledgment: Acknowledgment = mockk(relaxed = true)
    val message =
        CouponIssueRequestedMessage(
            requestId = 1L,
            couponId = 10L,
            userId = 100L,
            idempotencyKey = "coupon:10:user:100:action:ISSUE",
        )
    val listener =
        CouponIssueRequestKafkaListener(
            couponIssueRequestService = couponIssueRequestService,
            couponIssueRequestKafkaMetrics = couponIssueRequestKafkaMetrics,
        )
}

private fun couponIssueRequest(
    status: CouponIssueRequestStatus,
    couponIssueId: Long? = null,
    resultCode: CouponCommandResultCode? = null,
    failureReason: String? = null,
    lastDeliveryError: String? = null,
) = CouponIssueRequest(
    id = 1L,
    couponId = 10L,
    userId = 100L,
    idempotencyKey = "coupon:10:user:100:action:ISSUE",
    status = status,
    resultCode = resultCode,
    couponIssueId = couponIssueId,
    failureReason = failureReason,
    enqueuedAt = LocalDateTime.of(2026, 4, 7, 9, 1),
    processingStartedAt = LocalDateTime.of(2026, 4, 7, 9, 5),
    deliveryAttemptCount = 1,
    lastDeliveryError = lastDeliveryError,
    processedAt =
        if (status == CouponIssueRequestStatus.SUCCEEDED || status == CouponIssueRequestStatus.FAILED) {
            LocalDateTime.of(2026, 4, 7, 9, 10)
        } else {
            null
        },
    createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
    updatedAt = LocalDateTime.of(2026, 4, 7, 9, 10),
)
