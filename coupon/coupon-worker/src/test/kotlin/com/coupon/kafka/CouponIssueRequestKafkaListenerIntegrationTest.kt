package com.coupon.kafka

import com.coupon.coupon.CouponService
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.coupon.request.CouponIssueRequestedMessage
import com.coupon.coupon.request.command.CouponIssueRequestCommand
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.storage.rdb.coupon.CouponIssueJpaRepository
import com.coupon.storage.rdb.coupon.CouponJpaRepository
import com.coupon.storage.rdb.coupon.request.CouponIssueRequestJpaRepository
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
class CouponIssueRequestKafkaListenerIntegrationTest : CouponWorkerKafkaIntegrationSupport() {
    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    @Autowired
    private lateinit var couponIssueRequestService: CouponIssueRequestService

    @Autowired
    private lateinit var couponIssueRequestKafkaPublisher: CouponIssueRequestKafkaPublisher

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

    @BeforeEach
    fun setUp() {
        databaseCleaner.clean()
    }

    @Test
    fun `Kafka listener consumes the request message and issues the coupon`() {
        val user = userService.createUser(CouponWorkerFixtures.userCreateCommand(index = 3))
        val coupon = couponService.createCoupon(CouponWorkerFixtures.couponCreateCommand(totalQuantity = 1))
        val request =
            couponIssueRequestService.accept(
                CouponIssueRequestCommand.Accept(
                    couponId = coupon.id,
                    userId = user.id,
                ),
            )

        val marked = couponIssueRequestService.markEnqueuedAfterRelay(request.id)
        assertThat(marked).isTrue()

        couponIssueRequestKafkaPublisher.publish(
            CouponIssueRequestedMessage(
                requestId = request.id,
                couponId = coupon.id,
                userId = user.id,
                idempotencyKey = request.idempotencyKey,
            ),
        )

        awaitUntilAsserted {
            val persistedRequest = couponIssueRequestJpaRepository.findById(request.id).orElseThrow()
            assertThat(persistedRequest.status).isEqualTo(CouponIssueRequestStatus.SUCCEEDED)
            assertThat(persistedRequest.couponIssueId).isNotNull()

            val couponIssue = couponIssueJpaRepository.findById(persistedRequest.couponIssueId!!).orElseThrow()
            val persistedCoupon = couponJpaRepository.findById(coupon.id).orElseThrow()

            assertThat(couponIssue.userId).isEqualTo(user.id)
            assertThat(couponIssue.couponId).isEqualTo(coupon.id)
            assertThat(persistedCoupon.remainingQuantity).isZero()
        }
    }
}
