package com.coupon.coupon.restaurant

import com.coupon.coupon.CouponIssueFacade
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.fixture.RestaurantCouponFixtures
import com.coupon.enums.coupon.CouponIssueResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence

class RestaurantCouponFacadeTest :
    BehaviorSpec({
        given("RestaurantCouponFacade로 restaurant 기준 발급을 처리하면") {
            `when`("활성 레스토랑 쿠폰이 존재하면") {
                val restaurantCouponService = mockk<RestaurantCouponService>()
                val couponIssueFacade = mockk<CouponIssueFacade>()
                val restaurantCouponFacade =
                    RestaurantCouponFacade(
                        restaurantCouponService = restaurantCouponService,
                        couponIssueFacade = couponIssueFacade,
                    )
                val restaurantCoupon = RestaurantCouponFixtures.coupon(restaurantId = 101L, couponId = 2001L)
                val commandSlot = slot<CouponIssueCommand.Issue>()

                every { restaurantCouponService.getActiveRestaurantCoupon(restaurantCoupon.restaurantId) } returns restaurantCoupon
                every { couponIssueFacade.issue(capture(commandSlot)) } returns CouponIssueResult.SUCCESS

                val result = restaurantCouponFacade.issueByRestaurant(restaurantCoupon.restaurantId, userId = 77L)

                then("restaurantId로 조회한 couponId를 기존 발급 facade에 전달한다") {
                    result shouldBe CouponIssueResult.SUCCESS
                    commandSlot.captured shouldBe
                        CouponIssueCommand.Issue(
                            couponId = restaurantCoupon.couponId,
                            userId = 77L,
                        )
                    verifySequence {
                        restaurantCouponService.getActiveRestaurantCoupon(restaurantCoupon.restaurantId)
                        couponIssueFacade.issue(any())
                    }
                }
            }
        }
    })
