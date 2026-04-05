package com.coupon.coupon

import com.coupon.enums.coupon.CouponPreviewInapplicableReason
import com.coupon.enums.coupon.CouponStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CouponEligibilityEvaluator {
    fun evaluate(
        coupon: CouponDetail,
        orderAmount: Long,
        alreadyIssued: Boolean,
        now: LocalDateTime = LocalDateTime.now(),
    ): CouponPreviewInapplicableReason? {
        if (alreadyIssued) {
            return CouponPreviewInapplicableReason.ALREADY_ISSUED
        }

        if (coupon.status != CouponStatus.ACTIVE) {
            return CouponPreviewInapplicableReason.NOT_ACTIVE
        }

        if (coupon.remainingQuantity <= 0) {
            return CouponPreviewInapplicableReason.OUT_OF_STOCK
        }

        if (now.isBefore(coupon.availableAt) || now.isAfter(coupon.endAt)) {
            return CouponPreviewInapplicableReason.EXPIRED
        }

        if (coupon.minOrderAmount != null && orderAmount < coupon.minOrderAmount) {
            return CouponPreviewInapplicableReason.BELOW_MIN_ORDER_AMOUNT
        }

        return null
    }
}
