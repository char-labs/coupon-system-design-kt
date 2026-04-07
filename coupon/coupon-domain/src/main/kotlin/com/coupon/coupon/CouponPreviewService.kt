package com.coupon.coupon

import com.coupon.coupon.command.CouponPreviewCommand
import org.springframework.stereotype.Service

@Service
class CouponPreviewService(
    private val couponRepository: CouponRepository,
    private val couponIssueRepository: CouponIssueRepository,
    private val couponEligibilityEvaluator: CouponEligibilityEvaluator,
    private val couponDiscountCalculator: CouponDiscountCalculator,
) {
    /**
     * Read-only preview path.
     * It never changes coupon state and only combines eligibility + discount calculation.
     */
    fun preview(command: CouponPreviewCommand): CouponPreview {
        val coupon = couponRepository.findDetailById(command.couponId)
        val normalizedOrderAmount = command.orderAmount.coerceAtLeast(0)
        val alreadyIssued = couponIssueRepository.existsByUserIdAndCouponId(command.userId, command.couponId)
        val reason =
            couponEligibilityEvaluator.evaluate(
                coupon = coupon,
                orderAmount = normalizedOrderAmount,
                alreadyIssued = alreadyIssued,
            )

        return CouponPreview(
            couponId = coupon.id,
            couponCode = coupon.code,
            couponName = coupon.name,
            couponType = coupon.type,
            orderAmount = normalizedOrderAmount,
            applicable = reason == null,
            discountAmount =
                if (reason == null) {
                    couponDiscountCalculator.calculate(coupon, normalizedOrderAmount)
                } else {
                    0
                },
            reason = reason,
        )
    }
}
