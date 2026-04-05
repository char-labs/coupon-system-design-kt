package com.coupon.coupon

import com.coupon.coupon.fixture.CouponAvailabilityFixtures
import com.coupon.coupon.fixture.CouponPreviewCommandFixtures
import com.coupon.coupon.fixture.FixedCouponFixtures
import com.coupon.enums.coupon.CouponPreviewInapplicableReason
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class CouponEligibilityEvaluatorTest :
    BehaviorSpec({
        val evaluator = CouponEligibilityEvaluator()
        val referenceTime = LocalDateTime.of(2026, 1, 1, 0, 0)

        given("쿠폰 적용 가능 여부를 판단하면") {
            `when`("이미 발급한 쿠폰이고 다른 조건도 동시에 위반하면") {
                val coupon =
                    CouponAvailabilityFixtures.outOfStock(
                        CouponAvailabilityFixtures.inactive(
                            FixedCouponFixtures.standard(referenceTime = referenceTime),
                        ),
                    )

                then("이미 발급한 쿠폰 사유가 우선한다") {
                    evaluator.evaluate(
                        coupon = coupon,
                        orderAmount = 50_000L,
                        alreadyIssued = true,
                        now = referenceTime,
                    ) shouldBe CouponPreviewInapplicableReason.ALREADY_ISSUED
                }
            }

            `when`("비활성 쿠폰이면") {
                val coupon =
                    CouponAvailabilityFixtures.inactive(
                        FixedCouponFixtures.standard(referenceTime = referenceTime),
                    )

                then("NOT_ACTIVE를 반환한다") {
                    evaluator.evaluate(
                        coupon = coupon,
                        orderAmount = 50_000L,
                        alreadyIssued = false,
                        now = referenceTime,
                    ) shouldBe CouponPreviewInapplicableReason.NOT_ACTIVE
                }
            }

            `when`("재고가 없으면") {
                val coupon =
                    CouponAvailabilityFixtures.outOfStock(
                        FixedCouponFixtures.standard(referenceTime = referenceTime),
                    )

                then("OUT_OF_STOCK을 반환한다") {
                    evaluator.evaluate(
                        coupon = coupon,
                        orderAmount = 50_000L,
                        alreadyIssued = false,
                        now = referenceTime,
                    ) shouldBe CouponPreviewInapplicableReason.OUT_OF_STOCK
                }
            }

            `when`("사용 가능 시간이 아직 시작되지 않았으면") {
                val coupon =
                    CouponAvailabilityFixtures.future(
                        base = FixedCouponFixtures.standard(referenceTime = referenceTime),
                        referenceTime = referenceTime,
                    )

                then("EXPIRED를 반환한다") {
                    evaluator.evaluate(
                        coupon = coupon,
                        orderAmount = 50_000L,
                        alreadyIssued = false,
                        now = referenceTime,
                    ) shouldBe CouponPreviewInapplicableReason.EXPIRED
                }
            }

            `when`("사용 가능 시간이 이미 종료되었으면") {
                val coupon =
                    CouponAvailabilityFixtures.expired(
                        base = FixedCouponFixtures.standard(referenceTime = referenceTime),
                        referenceTime = referenceTime,
                    )

                then("EXPIRED를 반환한다") {
                    evaluator.evaluate(
                        coupon = coupon,
                        orderAmount = 50_000L,
                        alreadyIssued = false,
                        now = referenceTime,
                    ) shouldBe CouponPreviewInapplicableReason.EXPIRED
                }
            }

            `when`("최소 주문 금액보다 적게 주문하면") {
                val coupon =
                    CouponAvailabilityFixtures.minimumOrderRequired(
                        base = FixedCouponFixtures.standard(referenceTime = referenceTime),
                        minOrderAmount = 30_000L,
                    )
                val command =
                    CouponPreviewCommandFixtures.belowMinimumOrder(
                        couponId = coupon.id,
                        minOrderAmount = 30_000L,
                    )

                then("BELOW_MIN_ORDER_AMOUNT를 반환한다") {
                    evaluator.evaluate(
                        coupon = coupon,
                        orderAmount = command.orderAmount,
                        alreadyIssued = false,
                        now = referenceTime,
                    ) shouldBe CouponPreviewInapplicableReason.BELOW_MIN_ORDER_AMOUNT
                }
            }

            `when`("활성 쿠폰이고 최소 주문 금액과 재고를 만족하면") {
                val coupon =
                    CouponAvailabilityFixtures.minimumOrderRequired(
                        base = FixedCouponFixtures.singleQuantity(referenceTime = referenceTime),
                        minOrderAmount = 30_000L,
                    )
                val command =
                    CouponPreviewCommandFixtures.exactMinimumOrder(
                        couponId = coupon.id,
                        minOrderAmount = 30_000L,
                    )

                then("적용 불가 사유가 없다") {
                    evaluator
                        .evaluate(
                            coupon = coupon,
                            orderAmount = command.orderAmount,
                            alreadyIssued = false,
                            now = referenceTime,
                        ).shouldBeNull()
                }
            }
        }
    })
