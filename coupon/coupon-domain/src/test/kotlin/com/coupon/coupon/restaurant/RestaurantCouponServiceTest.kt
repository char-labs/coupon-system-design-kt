package com.coupon.coupon.restaurant

import com.coupon.coupon.CouponRepository
import com.coupon.coupon.fixture.CouponFixtures
import com.coupon.coupon.fixture.RestaurantCouponFixtures
import com.coupon.enums.coupon.RestaurantCouponStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import java.time.LocalDateTime

class RestaurantCouponServiceTest :
    BehaviorSpec({
        given("RestaurantCouponService로 맛집 쿠폰을 생성하면") {
            `when`("쿠폰이 존재하고 저장이 성공하면") {
                val context = RestaurantCouponServiceTestContext()
                val command = RestaurantCouponFixtures.createCommand(restaurantId = 101L, couponId = 2001L)
                val savedCoupon = RestaurantCouponFixtures.coupon(restaurantId = command.restaurantId, couponId = command.couponId)

                every { context.couponRepository.findById(command.couponId) } returns CouponFixtures.standard(id = command.couponId)
                every { context.restaurantCouponRepository.existsByRestaurantIdAndCouponId(command.restaurantId, command.couponId) } returns
                    false
                every { context.restaurantCouponRepository.save(any()) } returns savedCoupon

                val result = context.restaurantCouponService.createRestaurantCoupon(command)

                then("coupon 존재를 확인한 뒤 저장한다") {
                    result shouldBe savedCoupon
                    verifySequence {
                        context.couponRepository.findById(command.couponId)
                        context.restaurantCouponRepository.existsByRestaurantIdAndCouponId(command.restaurantId, command.couponId)
                        context.restaurantCouponRepository.save(any())
                    }
                }
            }
        }

        given("RestaurantCouponService로 맛집 쿠폰 배치를 생성하면") {
            `when`("1~3건 요청이면") {
                val context = RestaurantCouponServiceTestContext()
                val first = RestaurantCouponFixtures.createCommand(restaurantId = 101L, couponId = 2001L)
                val second = RestaurantCouponFixtures.createCommand(restaurantId = 102L, couponId = 2002L)
                val batch = RestaurantCouponFixtures.createBatch(first, second)

                every { context.couponRepository.findById(any()) } answers { CouponFixtures.standard(id = firstArg()) }
                every { context.restaurantCouponRepository.existsByRestaurantIdAndCouponId(first.restaurantId, first.couponId) } returns
                    false
                every { context.restaurantCouponRepository.existsByRestaurantIdAndCouponId(second.restaurantId, second.couponId) } returns
                    false
                every { context.restaurantCouponRepository.save(any()) } returnsMany
                    listOf(
                        RestaurantCouponFixtures.coupon(id = 1L, restaurantId = first.restaurantId, couponId = first.couponId),
                        RestaurantCouponFixtures.coupon(id = 2L, restaurantId = second.restaurantId, couponId = second.couponId),
                    )

                val result = context.restaurantCouponService.createRestaurantCoupons(batch)

                then("각 항목을 순서대로 생성한다") {
                    result.size shouldBe 2
                    verifySequence {
                        context.couponRepository.findById(first.couponId)
                        context.restaurantCouponRepository.existsByRestaurantIdAndCouponId(first.restaurantId, first.couponId)
                        context.restaurantCouponRepository.save(any())
                        context.couponRepository.findById(second.couponId)
                        context.restaurantCouponRepository.existsByRestaurantIdAndCouponId(second.restaurantId, second.couponId)
                        context.restaurantCouponRepository.save(any())
                    }
                }
            }

            `when`("4건 이상 요청이면") {
                val context = RestaurantCouponServiceTestContext()
                val batch =
                    RestaurantCouponFixtures.createBatch(
                        RestaurantCouponFixtures.createCommand(restaurantId = 101L, couponId = 2001L),
                        RestaurantCouponFixtures.createCommand(restaurantId = 102L, couponId = 2002L),
                        RestaurantCouponFixtures.createCommand(restaurantId = 103L, couponId = 2003L),
                        RestaurantCouponFixtures.createCommand(restaurantId = 104L, couponId = 2004L),
                    )

                val exception =
                    shouldThrow<ErrorException> {
                        context.restaurantCouponService.createRestaurantCoupons(batch)
                    }

                then("400 배치 크기 예외를 반환하고 저장하지 않는다") {
                    exception.errorType shouldBe ErrorType.INVALID_RESTAURANT_COUPON_BATCH_SIZE
                    verify(exactly = 0) { context.couponRepository.findById(any()) }
                    verify(exactly = 0) { context.restaurantCouponRepository.save(any()) }
                }
            }
        }

        given("RestaurantCouponService로 활성 맛집 쿠폰을 조회하면") {
            `when`("상태가 ACTIVE가 아니면") {
                val context = RestaurantCouponServiceTestContext()
                val restaurantCoupon =
                    RestaurantCouponFixtures.coupon(
                        status = RestaurantCouponStatus.INACTIVE,
                    )

                every { context.restaurantCouponRepository.findActiveByRestaurantId(restaurantCoupon.restaurantId) } returns
                    restaurantCoupon

                val exception =
                    shouldThrow<ErrorException> {
                        context.restaurantCouponService.getActiveRestaurantCoupon(restaurantCoupon.restaurantId)
                    }

                then("활성 상태 예외를 반환한다") {
                    exception.errorType shouldBe ErrorType.RESTAURANT_COUPON_NOT_ACTIVE
                }
            }

            `when`("유효 시간이 지났으면") {
                val context = RestaurantCouponServiceTestContext()
                val referenceTime = LocalDateTime.now()
                val restaurantCoupon =
                    RestaurantCouponFixtures.coupon(
                        availableAt = referenceTime.minusDays(2),
                        endAt = referenceTime.minusMinutes(1),
                    )

                every { context.restaurantCouponRepository.findActiveByRestaurantId(restaurantCoupon.restaurantId) } returns
                    restaurantCoupon

                val exception =
                    shouldThrow<ErrorException> {
                        context.restaurantCouponService.getActiveRestaurantCoupon(restaurantCoupon.restaurantId)
                    }

                then("만료 예외를 반환한다") {
                    exception.errorType shouldBe ErrorType.RESTAURANT_COUPON_EXPIRED
                }
            }
        }

        given("RestaurantCouponService로 맛집 쿠폰을 삭제하면") {
            `when`("쿠폰 id가 주어지면") {
                val context = RestaurantCouponServiceTestContext()
                justRun { context.restaurantCouponRepository.delete(any()) }

                context.restaurantCouponService.deleteRestaurantCoupon(1L)

                then("repository delete를 호출한다") {
                    verify(exactly = 1) { context.restaurantCouponRepository.delete(1L) }
                }
            }
        }
    }) {
    private class RestaurantCouponServiceTestContext {
        val restaurantCouponRepository = mockk<RestaurantCouponRepository>()
        val couponRepository = mockk<CouponRepository>()
        val restaurantCouponService =
            RestaurantCouponService(
                restaurantCouponRepository = restaurantCouponRepository,
                couponRepository = couponRepository,
            )
    }
}
