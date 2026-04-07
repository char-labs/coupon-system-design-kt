package com.coupon.coupon.request

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.event.CouponOutboxEventType
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
                val createdRequest = couponIssueRequest(id = 1L, couponId = command.couponId, userId = command.userId)
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
                val existingRequest = couponIssueRequest(id = 1L, couponId = command.couponId, userId = command.userId)

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
                val pendingRequest = couponIssueRequest(id = 10L, couponId = 11L, userId = 101L)
                val succeededRequest =
                    pendingRequest.copy(
                        status = CouponIssueRequestStatus.SUCCEEDED,
                        couponIssueId = 1000L,
                        processedAt = LocalDateTime.of(2026, 4, 7, 9, 10),
                    )
                val issuedCoupon =
                    CouponIssue(
                        id = 1000L,
                        couponId = pendingRequest.couponId,
                        userId = pendingRequest.userId,
                        status = CouponIssueStatus.ISSUED,
                    )

                every { context.couponIssueRequestRepository.findById(pendingRequest.id) } returnsMany
                    listOf(pendingRequest, pendingRequest, succeededRequest)
                every { context.couponIssueRequestRepository.markProcessing(pendingRequest.id, any()) } returns true
                every {
                    context.couponIssueService.executeIssue(
                        CouponIssueCommand.Issue(
                            couponId = pendingRequest.couponId,
                            userId = pendingRequest.userId,
                        ),
                    )
                } returns issuedCoupon
                every { context.couponIssueRequestRepository.markSucceeded(pendingRequest.id, issuedCoupon.id, any()) } returns true

                val result = context.service.process(pendingRequest.id)

                then("request를 SUCCEEDED로 마킹한다") {
                    result shouldBe CouponIssueRequestProcessingResult.Completed(succeededRequest)
                    verifySequence {
                        context.couponIssueRequestRepository.findById(pendingRequest.id)
                        context.couponIssueRequestRepository.findById(pendingRequest.id)
                        context.couponIssueRequestRepository.markProcessing(pendingRequest.id, any())
                        context.couponIssueService.executeIssue(
                            CouponIssueCommand.Issue(
                                couponId = pendingRequest.couponId,
                                userId = pendingRequest.userId,
                            ),
                        )
                        context.couponIssueRequestRepository.markSucceeded(pendingRequest.id, issuedCoupon.id, any())
                        context.couponIssueRequestRepository.findById(pendingRequest.id)
                    }
                }
            }

            `when`("발급이 비즈니스 실패로 끝나면") {
                val context = CouponIssueRequestServiceTestContext()
                val pendingRequest = couponIssueRequest(id = 10L, couponId = 11L, userId = 101L)
                val failedRequest =
                    pendingRequest.copy(
                        status = CouponIssueRequestStatus.FAILED,
                        resultCode = CouponCommandResultCode.ALREADY_ISSUED,
                        failureReason = ErrorType.ALREADY_ISSUED_COUPON.message,
                        processedAt = LocalDateTime.of(2026, 4, 7, 9, 10),
                    )

                every { context.couponIssueRequestRepository.findById(pendingRequest.id) } returnsMany
                    listOf(pendingRequest, pendingRequest, failedRequest)
                every { context.couponIssueRequestRepository.markProcessing(pendingRequest.id, any()) } returns true
                every {
                    context.couponIssueService.executeIssue(
                        CouponIssueCommand.Issue(
                            couponId = pendingRequest.couponId,
                            userId = pendingRequest.userId,
                        ),
                    )
                } throws ErrorException(ErrorType.ALREADY_ISSUED_COUPON)
                every {
                    context.couponIssueRequestRepository.markFailed(
                        pendingRequest.id,
                        CouponCommandResultCode.ALREADY_ISSUED,
                        ErrorType.ALREADY_ISSUED_COUPON.message,
                        any(),
                    )
                } returns true

                val result = context.service.process(pendingRequest.id)

                then("request를 FAILED로 마킹하고 outbox는 성공 처리할 수 있게 완료로 반환한다") {
                    result shouldBe CouponIssueRequestProcessingResult.Completed(failedRequest)
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

private fun couponIssueRequest(
    id: Long,
    couponId: Long,
    userId: Long,
    status: CouponIssueRequestStatus = CouponIssueRequestStatus.PENDING,
) = CouponIssueRequest(
    id = id,
    couponId = couponId,
    userId = userId,
    idempotencyKey = "coupon:$couponId:user:$userId:action:ISSUE",
    status = status,
    resultCode = null,
    couponIssueId = null,
    failureReason = null,
    processedAt = null,
    createdAt = LocalDateTime.of(2026, 4, 7, 9, 0),
    updatedAt = LocalDateTime.of(2026, 4, 7, 9, 0),
)
