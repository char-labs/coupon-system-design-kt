package com.coupon.coupon.request

import com.coupon.coupon.fixture.CouponIssueRequestFixtures
import com.coupon.coupon.support.DomainServiceTestRuntime
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.outbox.command.OutboxEventCommand
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class CouponIssueRequestReconciliationServiceTest :
    BehaviorSpec({
        given("CouponIssueRequestReconciliationService가 보정을 수행하면") {
            `when`("stale PENDING request에 활성 outbox가 없으면") {
                val context = CouponIssueRequestReconciliationServiceTestContext()
                val pendingRequest = CouponIssueRequestFixtures.pending(id = 1L, couponId = 10L, userId = 100L)
                val outboxSlot = slot<OutboxEventCommand.Publish>()

                every { context.couponIssueRequestRepository.recoverStuckProcessing(any()) } returns 2
                every {
                    context.couponIssueRequestRepository.findStaleByStatuses(
                        statuses = setOf(CouponIssueRequestStatus.PENDING),
                        updatedBefore = any(),
                        limit = 100,
                    )
                } returns listOf(pendingRequest)
                every {
                    context.outboxEventService.existsActiveEvent(
                        aggregateType = CouponIssueRequestOutboxCommandFactory.AGGREGATE_TYPE,
                        aggregateId = pendingRequest.id.toString(),
                    )
                } returns false
                every { context.outboxEventService.publish(capture(outboxSlot)) } returns mockk()
                every { context.couponIssueRequestRepository.findInconsistentSucceeded(100) } returns emptyList()

                val result =
                    context.service.reconcile(
                        processingUpdatedBefore = LocalDateTime.of(2026, 4, 7, 10, 0),
                        pendingUpdatedBefore = LocalDateTime.of(2026, 4, 7, 10, 0),
                        batchSize = 100,
                    )

                then("request를 재큐잉하고 요약을 반환한다") {
                    result shouldBe
                        CouponIssueRequestReconciliationSummary(
                            recoveredProcessingCount = 2,
                            scannedPendingCount = 1,
                            requeuedPendingCount = 1,
                            scannedInconsistentSucceededCount = 0,
                            isolatedInconsistentSucceededCount = 0,
                        )
                    outboxSlot.captured.eventType shouldBe CouponIssueRequestOutboxCommandFactory.issueRequested(pendingRequest).eventType
                    outboxSlot.captured.aggregateType shouldBe CouponIssueRequestOutboxCommandFactory.AGGREGATE_TYPE
                    outboxSlot.captured.aggregateId shouldBe pendingRequest.id.toString()
                    outboxSlot.captured.payloadJson shouldBe """{"requestId":${pendingRequest.id}}"""
                    outboxSlot.captured.dedupeKey shouldBe pendingRequest.idempotencyKey
                }
            }

            `when`("stale PENDING request에 활성 outbox가 있으면") {
                val context = CouponIssueRequestReconciliationServiceTestContext()
                val pendingRequest = CouponIssueRequestFixtures.pending(id = 2L, couponId = 11L, userId = 101L)

                every { context.couponIssueRequestRepository.recoverStuckProcessing(any()) } returns 0
                every {
                    context.couponIssueRequestRepository.findStaleByStatuses(
                        statuses = setOf(CouponIssueRequestStatus.PENDING),
                        updatedBefore = any(),
                        limit = 100,
                    )
                } returns listOf(pendingRequest)
                every {
                    context.outboxEventService.existsActiveEvent(
                        aggregateType = CouponIssueRequestOutboxCommandFactory.AGGREGATE_TYPE,
                        aggregateId = pendingRequest.id.toString(),
                    )
                } returns true
                every { context.couponIssueRequestRepository.findInconsistentSucceeded(100) } returns emptyList()

                val result =
                    context.service.reconcile(
                        processingUpdatedBefore = LocalDateTime.of(2026, 4, 7, 10, 0),
                        pendingUpdatedBefore = LocalDateTime.of(2026, 4, 7, 10, 0),
                        batchSize = 100,
                    )

                then("중복 outbox를 만들지 않는다") {
                    result.requeuedPendingCount shouldBe 0
                    verify(exactly = 0) { context.outboxEventService.publish(any()) }
                }
            }

            `when`("inconsistent SUCCEEDED request가 있으면") {
                val context = CouponIssueRequestReconciliationServiceTestContext()
                val succeededRequest =
                    CouponIssueRequestFixtures
                        .build(
                            id = 3L,
                            couponId = 12L,
                            userId = 102L,
                            status = CouponIssueRequestStatus.SUCCEEDED,
                        ).copy(couponIssueId = 999L)

                every { context.couponIssueRequestRepository.recoverStuckProcessing(any()) } returns 0
                every {
                    context.couponIssueRequestRepository.findStaleByStatuses(
                        statuses = setOf(CouponIssueRequestStatus.PENDING),
                        updatedBefore = any(),
                        limit = 100,
                    )
                } returns emptyList()
                every { context.couponIssueRequestRepository.findInconsistentSucceeded(100) } returns listOf(succeededRequest)
                every {
                    context.couponIssueRequestRepository.markDead(
                        requestId = succeededRequest.id,
                        candidateStatuses = setOf(CouponIssueRequestStatus.SUCCEEDED),
                        resultCode = CouponCommandResultCode.UNKNOWN_ERROR,
                        failureReason = CouponIssueRequestReconciliationService.INCONSISTENT_SUCCEEDED_FAILURE_REASON,
                        processedAt = any(),
                    )
                } returns true

                val result =
                    context.service.reconcile(
                        processingUpdatedBefore = LocalDateTime.of(2026, 4, 7, 10, 0),
                        pendingUpdatedBefore = LocalDateTime.of(2026, 4, 7, 10, 0),
                        batchSize = 100,
                    )

                then("요청을 DEAD로 격리한다") {
                    result shouldBe
                        CouponIssueRequestReconciliationSummary(
                            recoveredProcessingCount = 0,
                            scannedPendingCount = 0,
                            requeuedPendingCount = 0,
                            scannedInconsistentSucceededCount = 1,
                            isolatedInconsistentSucceededCount = 1,
                        )
                }
            }
        }
    })

private class CouponIssueRequestReconciliationServiceTestContext {
    val couponIssueRequestRepository = mockk<CouponIssueRequestRepository>()
    val outboxEventService = mockk<OutboxEventService>()
    val service =
        CouponIssueRequestReconciliationService(
            couponIssueRequestRepository = couponIssueRequestRepository,
            outboxEventService = outboxEventService,
        )

    init {
        DomainServiceTestRuntime.initialize()
    }
}
