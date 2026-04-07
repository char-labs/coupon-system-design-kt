package com.coupon.coupon.request

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.fixture.CouponIssueRequestFixtures
import com.coupon.coupon.request.command.CouponIssueRequestCommand
import com.coupon.coupon.request.criteria.CouponIssueRequestCriteria
import com.coupon.coupon.support.DomainServiceTestRuntime
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.enums.coupon.CouponIssueStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.outbox.command.OutboxEventCommand
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import java.time.LocalDateTime

class CouponIssueRequestServiceTest :
    BehaviorSpec({
        given("CouponIssueRequestService로 발급 요청을 접수하면") {
            `when`("같은 idempotency key가 없으면") {
                val context = CouponIssueRequestServiceTestContext()
                val command = CouponIssueRequestCommand.Accept(couponId = 10L, userId = 100L)
                val createdRequest = CouponIssueRequestFixtures.pending(id = 1L, couponId = command.couponId, userId = command.userId)
                val criteriaSlot = slot<CouponIssueRequestCriteria.Create>()
                val outboxSlot = slot<OutboxEventCommand.Publish>()

                every { context.couponIssueRequestRepository.saveIfAbsent(capture(criteriaSlot)) } returns
                    CouponIssueRequestPersistResult(createdRequest, created = true)
                every { context.outboxEventService.publish(capture(outboxSlot)) } returns mockk()

                val result = context.service.accept(command)

                then("request와 outbox를 함께 저장한다") {
                    result shouldBe createdRequest
                    criteriaSlot.captured shouldBe CouponIssueRequestCriteria.Create.of(command)
                    outboxSlot.captured.eventType shouldBe CouponOutboxEventType.COUPON_ISSUE_REQUESTED
                    outboxSlot.captured.aggregateType shouldBe "COUPON_ISSUE_REQUEST"
                    outboxSlot.captured.aggregateId shouldBe createdRequest.id.toString()
                    outboxSlot.captured.payloadJson shouldBe """{"requestId":${createdRequest.id}}"""
                    outboxSlot.captured.dedupeKey shouldBe createdRequest.idempotencyKey
                }
            }

            `when`("같은 idempotency key 요청이 이미 있으면") {
                val context = CouponIssueRequestServiceTestContext()
                val command = CouponIssueRequestCommand.Accept(couponId = 10L, userId = 100L)
                val existingRequest = CouponIssueRequestFixtures.pending(id = 1L, couponId = command.couponId, userId = command.userId)

                every { context.couponIssueRequestRepository.saveIfAbsent(any()) } returns
                    CouponIssueRequestPersistResult(existingRequest, created = false)

                val result = context.service.accept(command)

                then("기존 request를 반환하고 outbox는 다시 발행하지 않는다") {
                    result shouldBe existingRequest
                    verify(exactly = 0) { context.outboxEventService.publish(any()) }
                }
            }
        }

        given("CouponIssueRequestService로 발급 요청을 처리하면") {
            `when`("요청이 성공적으로 발급되면") {
                val context = CouponIssueRequestServiceTestContext()
                val enqueuedRequest =
                    CouponIssueRequestFixtures.enqueued(
                        id = 10L,
                        couponId = 11L,
                        userId = 101L,
                        enqueuedAt = LocalDateTime.of(2026, 4, 7, 9, 1),
                    )
                val succeededRequest =
                    enqueuedRequest.copy(
                        status = CouponIssueRequestStatus.SUCCEEDED,
                        couponIssueId = 1000L,
                        processingStartedAt = LocalDateTime.of(2026, 4, 7, 9, 5),
                        processedAt = LocalDateTime.of(2026, 4, 7, 9, 10),
                    )
                val issuedCoupon =
                    CouponIssue(
                        id = 1000L,
                        couponId = enqueuedRequest.couponId,
                        userId = enqueuedRequest.userId,
                        status = CouponIssueStatus.ISSUED,
                    )

                every { context.couponIssueRequestRepository.findById(enqueuedRequest.id) } returnsMany
                    listOf(enqueuedRequest, enqueuedRequest, succeededRequest)
                every { context.couponIssueRequestRepository.markProcessing(enqueuedRequest.id, any(), any()) } returns true
                every {
                    context.couponIssueService.executeIssue(
                        CouponIssueCommand.Issue(
                            couponId = enqueuedRequest.couponId,
                            userId = enqueuedRequest.userId,
                        ),
                    )
                } returns issuedCoupon
                every { context.couponIssueRequestRepository.markSucceeded(enqueuedRequest.id, issuedCoupon.id, any()) } returns true

                val result = context.service.process(enqueuedRequest.id)

                then("request를 SUCCEEDED로 마킹한다") {
                    result shouldBe CouponIssueRequestProcessingResult.Completed(succeededRequest)
                    verifySequence {
                        context.couponIssueRequestRepository.findById(enqueuedRequest.id)
                        context.couponIssueRequestRepository.findById(enqueuedRequest.id)
                        context.couponIssueRequestRepository.markProcessing(enqueuedRequest.id, any(), any())
                        context.couponIssueService.executeIssue(
                            CouponIssueCommand.Issue(
                                couponId = enqueuedRequest.couponId,
                                userId = enqueuedRequest.userId,
                            ),
                        )
                        context.couponIssueRequestRepository.markSucceeded(enqueuedRequest.id, issuedCoupon.id, any())
                        context.couponIssueRequestRepository.findById(enqueuedRequest.id)
                    }
                }
            }

            `when`("발급이 비즈니스 실패로 끝나면") {
                val context = CouponIssueRequestServiceTestContext()
                val enqueuedRequest =
                    CouponIssueRequestFixtures.enqueued(
                        id = 10L,
                        couponId = 11L,
                        userId = 101L,
                        enqueuedAt = LocalDateTime.of(2026, 4, 7, 9, 1),
                    )
                val failedRequest =
                    enqueuedRequest.copy(
                        status = CouponIssueRequestStatus.FAILED,
                        resultCode = CouponCommandResultCode.ALREADY_ISSUED,
                        failureReason = ErrorType.ALREADY_ISSUED_COUPON.message,
                        processingStartedAt = LocalDateTime.of(2026, 4, 7, 9, 5),
                        processedAt = LocalDateTime.of(2026, 4, 7, 9, 10),
                    )

                every { context.couponIssueRequestRepository.findById(enqueuedRequest.id) } returnsMany
                    listOf(enqueuedRequest, enqueuedRequest, failedRequest)
                every { context.couponIssueRequestRepository.markProcessing(enqueuedRequest.id, any(), any()) } returns true
                every {
                    context.couponIssueService.executeIssue(
                        CouponIssueCommand.Issue(
                            couponId = enqueuedRequest.couponId,
                            userId = enqueuedRequest.userId,
                        ),
                    )
                } throws ErrorException(ErrorType.ALREADY_ISSUED_COUPON)
                every {
                    context.couponIssueRequestRepository.markFailed(
                        enqueuedRequest.id,
                        CouponCommandResultCode.ALREADY_ISSUED,
                        ErrorType.ALREADY_ISSUED_COUPON.message,
                        any(),
                    )
                } returns true

                val result = context.service.process(enqueuedRequest.id)

                then("request를 FAILED로 마킹하고 outbox는 성공 처리할 수 있게 완료로 반환한다") {
                    result shouldBe CouponIssueRequestProcessingResult.Completed(failedRequest)
                }
            }

            `when`("발급 중 재시도 가능한 예외가 발생하면") {
                val context = CouponIssueRequestServiceTestContext()
                val enqueuedRequest =
                    CouponIssueRequestFixtures.enqueued(
                        id = 10L,
                        couponId = 11L,
                        userId = 101L,
                        enqueuedAt = LocalDateTime.of(2026, 4, 7, 9, 1),
                    )
                val requeuedRequest =
                    enqueuedRequest.copy(
                        status = CouponIssueRequestStatus.ENQUEUED,
                        deliveryAttemptCount = 1,
                        lastDeliveryError = "ErrorException: ${ErrorType.LOCK_ACQUISITION_FAILED.message}",
                    )

                every { context.couponIssueRequestRepository.findById(enqueuedRequest.id) } returnsMany
                    listOf(enqueuedRequest, enqueuedRequest, requeuedRequest)
                every { context.couponIssueRequestRepository.markProcessing(enqueuedRequest.id, any(), any()) } returns true
                every {
                    context.couponIssueService.executeIssue(
                        CouponIssueCommand.Issue(
                            couponId = enqueuedRequest.couponId,
                            userId = enqueuedRequest.userId,
                        ),
                    )
                } throws ErrorException(ErrorType.LOCK_ACQUISITION_FAILED)
                every {
                    context.couponIssueRequestRepository.markEnqueuedForRetry(
                        requestId = enqueuedRequest.id,
                        lastDeliveryError = "ErrorException: ${ErrorType.LOCK_ACQUISITION_FAILED.message}",
                        candidateStatuses = setOf(CouponIssueRequestStatus.PROCESSING),
                        enqueuedAt = any(),
                    )
                } returns true

                val result = context.service.process(enqueuedRequest.id)

                then("request를 ENQUEUED로 되돌리고 retry 결과를 반환한다") {
                    result shouldBe
                        CouponIssueRequestProcessingResult.Retry(
                            reason = "ErrorException: ${ErrorType.LOCK_ACQUISITION_FAILED.message}",
                            request = requeuedRequest,
                            transitioned = true,
                        )
                }
            }

            `when`("요청이 없으면") {
                val context = CouponIssueRequestServiceTestContext()

                every { context.couponIssueRequestRepository.findById(999L) } throws ErrorException(ErrorType.NOT_FOUND_DATA)

                val result = context.service.process(999L)

                then("dead 처리 사유를 반환한다") {
                    result shouldBe CouponIssueRequestProcessingResult.Dead("Coupon issue request 999 was not found")
                }
            }
        }
    }) {
    private class CouponIssueRequestServiceTestContext {
        init {
            DomainServiceTestRuntime.initialize()
        }

        val couponIssueRequestRepository = mockk<CouponIssueRequestRepository>()
        val outboxEventService = mockk<OutboxEventService>()
        val couponIssueService = mockk<CouponIssueService>()
        val service =
            CouponIssueRequestService(
                couponIssueRequestRepository = couponIssueRequestRepository,
                outboxEventService = outboxEventService,
                couponIssueService = couponIssueService,
            )
    }
}
