package com.coupon.coupon

import com.coupon.coupon.command.CouponPreviewCommand
import com.coupon.enums.coupon.CouponPreviewInapplicableReason
import com.coupon.enums.coupon.CouponStatus
import com.coupon.enums.coupon.CouponType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CouponPreviewServiceTest {
    @Mock
    private lateinit var couponRepository: CouponRepository

    @Mock
    private lateinit var couponIssueRepository: CouponIssueRepository

    private lateinit var couponPreviewService: CouponPreviewService

    @BeforeEach
    fun setUp() {
        couponPreviewService =
            CouponPreviewService(
                couponRepository = couponRepository,
                couponIssueRepository = couponIssueRepository,
                couponEligibilityEvaluator = CouponEligibilityEvaluator(),
                couponDiscountCalculator = CouponDiscountCalculator(),
            )
    }

    @Test
    fun `정액 쿠폰은 주문 금액 내에서 할인 금액을 계산한다`() {
        stubCoupon(
            couponDetail(
                type = CouponType.FIXED,
                discountAmount = 5_000,
            ),
        )
        whenever(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false)

        val preview = couponPreviewService.preview(CouponPreviewCommand(couponId = 1L, userId = 1L, orderAmount = 4_000))

        assertThat(preview.applicable).isTrue()
        assertThat(preview.discountAmount).isEqualTo(4_000)
        assertThat(preview.reason).isNull()
    }

    @Test
    fun `정률 쿠폰은 최대 할인 금액을 넘지 않는다`() {
        stubCoupon(
            couponDetail(
                type = CouponType.PERCENTAGE,
                discountAmount = 30,
                maxDiscountAmount = 10_000,
            ),
        )
        whenever(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false)

        val preview = couponPreviewService.preview(CouponPreviewCommand(couponId = 1L, userId = 1L, orderAmount = 50_000))

        assertThat(preview.applicable).isTrue()
        assertThat(preview.discountAmount).isEqualTo(10_000)
        assertThat(preview.reason).isNull()
    }

    @Test
    fun `이미 발급한 쿠폰은 적용 불가로 응답한다`() {
        stubCoupon(couponDetail())
        whenever(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(true)

        val preview = couponPreviewService.preview(CouponPreviewCommand(couponId = 1L, userId = 1L, orderAmount = 50_000))

        assertThat(preview.applicable).isFalse()
        assertThat(preview.discountAmount).isZero()
        assertThat(preview.reason).isEqualTo(CouponPreviewInapplicableReason.ALREADY_ISSUED)
    }

    @Test
    fun `최소 주문 금액을 만족하지 못하면 적용 불가다`() {
        stubCoupon(couponDetail(minOrderAmount = 30_000))
        whenever(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false)

        val preview = couponPreviewService.preview(CouponPreviewCommand(couponId = 1L, userId = 1L, orderAmount = 29_999))

        assertThat(preview.applicable).isFalse()
        assertThat(preview.discountAmount).isZero()
        assertThat(preview.reason).isEqualTo(CouponPreviewInapplicableReason.BELOW_MIN_ORDER_AMOUNT)
    }

    @Test
    fun `재고가 없으면 적용 불가다`() {
        stubCoupon(couponDetail(remainingQuantity = 0))
        whenever(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false)

        val preview = couponPreviewService.preview(CouponPreviewCommand(couponId = 1L, userId = 1L, orderAmount = 50_000))

        assertThat(preview.applicable).isFalse()
        assertThat(preview.discountAmount).isZero()
        assertThat(preview.reason).isEqualTo(CouponPreviewInapplicableReason.OUT_OF_STOCK)
    }

    @Test
    fun `활성 기간이 아니면 적용 불가다`() {
        stubCoupon(
            couponDetail(
                availableAt = LocalDateTime.now().plusDays(1),
                endAt = LocalDateTime.now().plusDays(2),
            ),
        )
        whenever(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false)

        val preview = couponPreviewService.preview(CouponPreviewCommand(couponId = 1L, userId = 1L, orderAmount = 50_000))

        assertThat(preview.applicable).isFalse()
        assertThat(preview.discountAmount).isZero()
        assertThat(preview.reason).isEqualTo(CouponPreviewInapplicableReason.EXPIRED)
    }

    @Test
    fun `비활성 쿠폰은 적용 불가다`() {
        stubCoupon(couponDetail(status = CouponStatus.INACTIVE))
        whenever(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false)

        val preview = couponPreviewService.preview(CouponPreviewCommand(couponId = 1L, userId = 1L, orderAmount = 50_000))

        assertThat(preview.applicable).isFalse()
        assertThat(preview.discountAmount).isZero()
        assertThat(preview.reason).isEqualTo(CouponPreviewInapplicableReason.NOT_ACTIVE)
    }

    private fun stubCoupon(couponDetail: CouponDetail) {
        whenever(couponRepository.findDetailById(1L)).thenReturn(couponDetail)
    }

    private fun couponDetail(
        status: CouponStatus = CouponStatus.ACTIVE,
        type: CouponType = CouponType.FIXED,
        discountAmount: Long = 5_000,
        maxDiscountAmount: Long? = null,
        minOrderAmount: Long? = null,
        remainingQuantity: Long = 10,
        availableAt: LocalDateTime = LocalDateTime.now().minusDays(1),
        endAt: LocalDateTime = LocalDateTime.now().plusDays(1),
    ) = CouponDetail(
        id = 1L,
        code = "CP-TEST",
        name = "테스트 쿠폰",
        type = type,
        status = status,
        discountAmount = discountAmount,
        maxDiscountAmount = maxDiscountAmount,
        minOrderAmount = minOrderAmount,
        totalQuantity = 10,
        remainingQuantity = remainingQuantity,
        availableAt = availableAt,
        endAt = endAt,
        createdAt = LocalDateTime.now().minusDays(2),
        updatedAt = LocalDateTime.now().minusDays(1),
    )
}
