package com.coupon.kafka

import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.CouponIssueRedisRepository
import com.coupon.coupon.CouponService
import com.coupon.coupon.execution.CouponIssueExecutionFacade
import com.coupon.coupon.execution.CouponIssueExecutionResult
import com.coupon.coupon.intake.CouponIssueMessage
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.shared.page.OffsetPageRequest
import com.coupon.support.testing.CouponWorkerFixtures
import com.coupon.support.testing.CouponWorkerIntegrationTest
import com.coupon.support.testing.DatabaseCleaner
import com.coupon.user.UserService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@CouponWorkerIntegrationTest
class CouponIssueWorkerFlowIntegrationTest : BehaviorSpec() {
    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var couponIssueExecutionFacade: CouponIssueExecutionFacade

    @Autowired
    private lateinit var couponIssueService: CouponIssueService

    @Autowired
    private lateinit var couponIssueRedisRepository: CouponIssueRedisRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    init {
        beforeTest {
            databaseCleaner.clean()
        }

        given("worker 발급 경로를 실행하면") {
            `when`("accepted message를 소비해 발급이 성공하면") {
                then("remaining_quantity를 줄이고 coupon_issue row를 저장한다") {
                    val coupon = couponService.createCoupon(CouponWorkerFixtures.couponCreateCommand(totalQuantity = 1L))
                    val user = userService.createUser(CouponWorkerFixtures.userCreateCommand(index = 1))

                    val result =
                        couponIssueExecutionFacade.execute(
                            CouponIssueMessage(
                                couponId = coupon.id,
                                userId = user.id,
                                requestId = "request-1",
                                acceptedAt = Instant.parse("2026-04-08T00:00:00Z"),
                            ),
                        )

                    (result is CouponIssueExecutionResult.Succeeded) shouldBe true
                    couponService.getCoupon(coupon.id).remainingQuantity.shouldBeExactly(0L)
                    couponIssueService.getCouponIssues(coupon.id, OffsetPageRequest(0, 20)).totalCount.shouldBeExactly(1L)
                }
            }

            `when`("Redis state를 비운 뒤 다시 reserve를 시도하면") {
                then("DB truth를 기준으로 duplicate state를 재구성한다") {
                    val coupon = couponService.createCoupon(CouponWorkerFixtures.couponCreateCommand(totalQuantity = 2L))
                    val user = userService.createUser(CouponWorkerFixtures.userCreateCommand(index = 1))

                    couponIssueExecutionFacade.execute(
                        CouponIssueMessage(
                            couponId = coupon.id,
                            userId = user.id,
                            requestId = "request-2",
                            acceptedAt = Instant.parse("2026-04-08T00:00:00Z"),
                        ),
                    )
                    couponIssueRedisRepository.clear(coupon.id)

                    val result = couponIssueService.reserveIssue(couponService.getCoupon(coupon.id), user.id)

                    result shouldBe CouponIssueResult.DUPLICATE
                }
            }
        }
    }
}
