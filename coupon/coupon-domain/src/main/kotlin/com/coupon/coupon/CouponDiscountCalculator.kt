package com.coupon.coupon

import com.coupon.enums.coupon.CouponType
import org.springframework.stereotype.Component

@Component
class CouponDiscountCalculator {
    fun calculate(
        coupon: CouponDetail,
        orderAmount: Long,
    ): Long {
        val normalizedOrderAmount = orderAmount.coerceAtLeast(0)
        val calculatedDiscount =
            when (coupon.type) {
                CouponType.FIXED -> coupon.discountAmount.coerceAtMost(normalizedOrderAmount)
                CouponType.PERCENTAGE -> normalizedOrderAmount * coupon.discountAmount / 100
            }

        return coupon.maxDiscountAmount?.let { calculatedDiscount.coerceAtMost(it) } ?: calculatedDiscount
    }
}
