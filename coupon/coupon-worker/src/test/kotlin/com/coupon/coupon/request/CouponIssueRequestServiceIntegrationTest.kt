package com.coupon.coupon.request

import com.coupon.coupon.CouponService
import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.request.command.CouponIssueRequestCommand
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.storage.rdb.coupon.CouponIssueJpaRepository
import com.coupon.storage.rdb.coupon.CouponJpaRepository
import com.coupon.storage.rdb.coupon.request.CouponIssueRequestJpaRepository
import com.coupon.storage.rdb.outbox.OutboxEventJpaRepository
import com.coupon.support.testing.CouponWorkerFixtures
import com.coupon.support.testing.CouponWorkerIntegrationTest
import com.coupon.support.testing.CouponWorkerKafkaIntegrationSupport
import com.coupon.support.testing.DatabaseCleaner
import com.coupon.user.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@CouponWorkerIntegrationTest
class CouponIssueRequestServiceIntegrationTest : CouponWorkerKafkaIntegrationSupport() {
    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    @Autowired
    private lateinit var couponIssueRequestService: CouponIssueRequestService

    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository

    @Autowired
    private lateinit var couponIssueJpaRepository: CouponIssueJpaRepository

    @Autowired
    private lateinit var couponJpaRepository: CouponJpaRepository

    @Autowired
    private lateinit var outboxEventJpaRepository: OutboxEventJpaRepository

    @BeforeEach
    fun setUp() {
        databaseCleaner.clean()
    }

    @Test
    fun `accept stores request and outbox in the same flow and reuses the idempotent request`() {
        val user = userService.createUser(CouponWorkerFixtures.userCreateCommand(index = 1))
        val coupon = couponService.createCoupon(CouponWorkerFixtures.couponCreateCommand(totalQuantity = 1))
        val command = CouponIssueRequestCommand.Accept(couponId = coupon.id, userId = user.id)

        val created = couponIssueRequestService.accept(command)
        val duplicated = couponIssueRequestService.accept(command)

        assertThat(duplicated.id).isEqualTo(created.id)

        val persisted = couponIssueRequestJpaRepository.findById(created.id).orElseThrow()
        assertThat(persisted.status).isEqualTo(CouponIssueRequestStatus.PENDING)
        assertThat(persisted.idempotencyKey).isEqualTo(command.idempotencyKey)

        val outboxEvent =
            outboxEventJpaRepository.findAll().single {
                it.eventType == CouponOutboxEventType.COUPON_ISSUE_REQUESTED &&
                    it.aggregateId == created.id.toString()
            }
        assertThat(outboxEvent.dedupeKey).isEqualTo(created.idempotencyKey)
    }

    @Test
    fun `process issues the coupon and marks the request as succeeded`() {
        val user = userService.createUser(CouponWorkerFixtures.userCreateCommand(index = 2))
        val coupon = couponService.createCoupon(CouponWorkerFixtures.couponCreateCommand(totalQuantity = 1))
        val request =
            couponIssueRequestService.accept(
                CouponIssueRequestCommand.Accept(
                    couponId = coupon.id,
                    userId = user.id,
                ),
            )

        val marked = couponIssueRequestService.markEnqueuedAfterRelay(request.id)
        val result = couponIssueRequestService.process(request.id)

        assertThat(marked).isTrue()
        assertThat(result).isInstanceOf(CouponIssueRequestProcessingResult.Completed::class.java)

        val completed = result as CouponIssueRequestProcessingResult.Completed
        assertThat(completed.request.status).isEqualTo(CouponIssueRequestStatus.SUCCEEDED)
        assertThat(completed.request.couponIssueId).isNotNull()

        val persistedRequest = couponIssueRequestJpaRepository.findById(request.id).orElseThrow()
        val couponIssue = couponIssueJpaRepository.findById(persistedRequest.couponIssueId!!).orElseThrow()
        val persistedCoupon = couponJpaRepository.findById(coupon.id).orElseThrow()

        assertThat(couponIssue.couponId).isEqualTo(coupon.id)
        assertThat(couponIssue.userId).isEqualTo(user.id)
        assertThat(persistedCoupon.remainingQuantity).isZero()
    }
}
