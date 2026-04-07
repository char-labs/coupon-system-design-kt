package com.coupon.kafka

import com.coupon.coupon.CouponService
import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.coupon.request.command.CouponIssueRequestCommand
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.outbox.OutboxPoller
import com.coupon.storage.rdb.coupon.CouponIssueJpaRepository
import com.coupon.storage.rdb.coupon.request.CouponIssueRequestJpaRepository
import com.coupon.storage.rdb.outbox.OutboxEventJpaRepository
import com.coupon.support.outbox.OutboxEventStatus
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
class CouponIssueRequestKafkaWorkflowIntegrationTest : CouponWorkerKafkaIntegrationSupport() {
    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    @Autowired
    private lateinit var couponIssueRequestService: CouponIssueRequestService

    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var outboxPoller: OutboxPoller

    @Autowired
    private lateinit var couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository

    @Autowired
    private lateinit var couponIssueJpaRepository: CouponIssueJpaRepository

    @Autowired
    private lateinit var outboxEventJpaRepository: OutboxEventJpaRepository

    @BeforeEach
    fun setUp() {
        databaseCleaner.clean()
    }

    @Test
    fun `accepted request is relayed through outbox and Kafka to a succeeded issue`() {
        val user = userService.createUser(CouponWorkerFixtures.userCreateCommand(index = 4))
        val coupon = couponService.createCoupon(CouponWorkerFixtures.couponCreateCommand(totalQuantity = 1))
        val request =
            couponIssueRequestService.accept(
                CouponIssueRequestCommand.Accept(
                    couponId = coupon.id,
                    userId = user.id,
                ),
            )

        val requestOutboxBeforeRelay =
            outboxEventJpaRepository.findAll().single {
                it.eventType == CouponOutboxEventType.COUPON_ISSUE_REQUESTED &&
                    it.aggregateId == request.id.toString()
            }
        assertThat(requestOutboxBeforeRelay.status).isEqualTo(OutboxEventStatus.PENDING)

        outboxPoller.poll()

        awaitUntilAsserted {
            val persistedRequest = couponIssueRequestJpaRepository.findById(request.id).orElseThrow()
            assertThat(persistedRequest.status).isEqualTo(CouponIssueRequestStatus.SUCCEEDED)
            assertThat(persistedRequest.couponIssueId).isNotNull()

            val requestOutboxAfterRelay =
                outboxEventJpaRepository.findAll().single {
                    it.eventType == CouponOutboxEventType.COUPON_ISSUE_REQUESTED &&
                        it.aggregateId == request.id.toString()
                }
            assertThat(requestOutboxAfterRelay.status).isEqualTo(OutboxEventStatus.SUCCEEDED)

            val couponIssue = couponIssueJpaRepository.findById(persistedRequest.couponIssueId!!).orElseThrow()
            assertThat(couponIssue.userId).isEqualTo(user.id)
            assertThat(couponIssue.couponId).isEqualTo(coupon.id)
        }
    }
}
