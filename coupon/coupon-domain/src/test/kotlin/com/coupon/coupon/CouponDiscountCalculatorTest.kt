package com.coupon.coupon

import com.coupon.coupon.fixture.FixedCouponFixtures
import com.coupon.coupon.fixture.PercentageCouponFixtures
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CouponDiscountCalculatorTest :
    BehaviorSpec({
        val calculator = CouponDiscountCalculator()

        given("쿠폰 할인 금액을 계산하면") {
            `when`("정액 쿠폰이고 주문 금액이 할인 금액보다 크면") {
                val coupon = FixedCouponFixtures.standard(discountAmount = 5_000L)

                then("쿠폰 할인 금액만큼 할인한다") {
                    calculator.calculate(coupon, 20_000L) shouldBe 5_000L
                }
            }

            `when`("정액 쿠폰이고 주문 금액이 할인 금액보다 작으면") {
                val coupon = FixedCouponFixtures.standard(discountAmount = 5_000L)

                then("주문 금액까지만 할인한다") {
                    calculator.calculate(coupon, 4_000L) shouldBe 4_000L
                }
            }

            `when`("정률 쿠폰에 최대 할인 금액이 없으면") {
                val coupon = PercentageCouponFixtures.standard(discountRate = 30L)

                then("주문 금액 비율만큼 할인한다") {
                    calculator.calculate(coupon, 50_000L) shouldBe 15_000L
                }
            }

            `when`("정률 쿠폰에 최대 할인 금액이 있으면") {
                val coupon = PercentageCouponFixtures.capped(discountRate = 30L, maxDiscountAmount = 10_000L)

                then("최대 할인 금액까지만 할인한다") {
                    calculator.calculate(coupon, 50_000L) shouldBe 10_000L
                }
            }

            `when`("주문 금액이 음수면") {
                val coupon = FixedCouponFixtures.standard(discountAmount = 5_000L)

                then("0원 주문으로 정규화해 계산한다") {
                    calculator.calculate(coupon, -1_000L) shouldBe 0L
                }
            }
        }
    })
