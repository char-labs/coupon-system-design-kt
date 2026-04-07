package com.coupon.coupon

import com.coupon.coupon.command.CouponPreviewCommand
import com.coupon.coupon.fixture.CouponAvailabilityFixtures
import com.coupon.coupon.fixture.CouponPreviewCommandFixtures
import com.coupon.coupon.fixture.FixedCouponFixtures
import com.coupon.enums.coupon.CouponPreviewInapplicableReason
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CouponServicePreviewTest :
    BehaviorSpec({
        given("쿠폰 미리보기를 요청하면") {
            `when`("적용 가능한 정액 쿠폰이면") {
                val context = CouponServicePreviewTestContext()
                val coupon = FixedCouponFixtures.standard(id = 101L, discountAmount = 5_000L)
                val command = CouponPreviewCommandFixtures.standard(couponId = coupon.id, orderAmount = 20_000L)

                val preview = context.preview(couponDetail = coupon, command = command)

                then("쿠폰 메타데이터와 할인 금액을 함께 반환한다") {
                    preview.couponId shouldBe coupon.id
                    preview.couponCode shouldBe coupon.code
                    preview.couponName shouldBe coupon.name
                    preview.couponType shouldBe coupon.type
                    preview.orderAmount shouldBe 20_000L
                    preview.applicable shouldBe true
                    preview.discountAmount shouldBe 5_000L
                    preview.reason.shouldBeNull()
                }
            }

            `when`("이미 발급한 쿠폰이면") {
                val context = CouponServicePreviewTestContext()
                val coupon = FixedCouponFixtures.standard(id = 202L, discountAmount = 5_000L)
                val command = CouponPreviewCommandFixtures.standard(couponId = coupon.id, orderAmount = 50_000L)

                val preview = context.preview(couponDetail = coupon, command = command, alreadyIssued = true)

                then("적용 불가 사유와 0원 할인을 반환한다") {
                    preview.couponId shouldBe coupon.id
                    preview.couponCode shouldBe coupon.code
                    preview.couponName shouldBe coupon.name
                    preview.applicable shouldBe false
                    preview.discountAmount.shouldBeZero()
                    preview.reason shouldBe CouponPreviewInapplicableReason.ALREADY_ISSUED
                }
            }

            `when`("주문 금액이 음수면") {
                val context = CouponServicePreviewTestContext()
                val coupon = FixedCouponFixtures.standard(discountAmount = 5_000L)
                val command = CouponPreviewCommandFixtures.negativeOrder(couponId = coupon.id)

                val preview = context.preview(couponDetail = coupon, command = command)

                then("0원 주문으로 정규화해 미리보기를 계산한다") {
                    preview.orderAmount shouldBe 0L
                    preview.discountAmount.shouldBeZero()
                    preview.reason.shouldBeNull()
                }
            }

            listOf(
                PreviewInapplicableCase(
                    description = "최소 주문 금액을 만족하지 못하면",
                    couponDetail =
                        CouponAvailabilityFixtures.minimumOrderRequired(
                            base = FixedCouponFixtures.standard(),
                            minOrderAmount = 30_000L,
                        ),
                    command = CouponPreviewCommandFixtures.belowMinimumOrder(minOrderAmount = 30_000L),
                    expectedReason = CouponPreviewInapplicableReason.BELOW_MIN_ORDER_AMOUNT,
                ),
                PreviewInapplicableCase(
                    description = "재고가 없으면",
                    couponDetail =
                        CouponAvailabilityFixtures.outOfStock(
                            base = FixedCouponFixtures.standard(),
                        ),
                    expectedReason = CouponPreviewInapplicableReason.OUT_OF_STOCK,
                ),
                PreviewInapplicableCase(
                    description = "활성 기간이 아니면",
                    couponDetail =
                        CouponAvailabilityFixtures.future(
                            base = FixedCouponFixtures.standard(),
                        ),
                    expectedReason = CouponPreviewInapplicableReason.EXPIRED,
                ),
                PreviewInapplicableCase(
                    description = "비활성 쿠폰이면",
                    couponDetail =
                        CouponAvailabilityFixtures.inactive(
                            base = FixedCouponFixtures.standard(),
                        ),
                    expectedReason = CouponPreviewInapplicableReason.NOT_ACTIVE,
                ),
            ).forEach { case ->
                `when`(case.description) {
                    val context = CouponServicePreviewTestContext()
                    val preview =
                        context.preview(
                            couponDetail = case.couponDetail,
                            command = case.command.copy(couponId = case.couponDetail.id),
                            alreadyIssued = case.alreadyIssued,
                        )

                    then("적용 불가 사유와 0원 할인을 반환한다") {
                        preview.applicable shouldBe false
                        preview.discountAmount.shouldBeZero()
                        preview.reason shouldBe case.expectedReason
                    }
                }
            }
        }
    }) {
    private class CouponServicePreviewTestContext {
        private val couponRepository = mockk<CouponRepository>()
        private val couponIssueRepository = mockk<CouponIssueRepository>()
        private val couponService =
            CouponService(
                couponRepository = couponRepository,
                couponIssueRepository = couponIssueRepository,
                couponCodeGenerator = mockk(relaxed = true),
                couponValidator = mockk(relaxed = true),
                couponEligibilityEvaluator = CouponEligibilityEvaluator(),
                couponDiscountCalculator = CouponDiscountCalculator(),
            )

        fun preview(
            couponDetail: CouponDetail = FixedCouponFixtures.standard(),
            command: CouponPreviewCommand = CouponPreviewCommandFixtures.standard(couponId = couponDetail.id),
            alreadyIssued: Boolean = false,
        ): CouponPreview {
            every { couponRepository.findDetailById(command.couponId) } returns couponDetail
            every { couponIssueRepository.existsByUserIdAndCouponId(command.userId, command.couponId) } returns alreadyIssued

            return couponService.preview(command)
        }
    }

    private data class PreviewInapplicableCase(
        val description: String,
        val couponDetail: CouponDetail = FixedCouponFixtures.standard(),
        val command: CouponPreviewCommand = CouponPreviewCommandFixtures.standard(couponId = couponDetail.id),
        val alreadyIssued: Boolean = false,
        val expectedReason: CouponPreviewInapplicableReason,
    )
}
