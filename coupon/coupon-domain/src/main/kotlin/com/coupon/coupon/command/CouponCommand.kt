package com.coupon.coupon.command

import com.coupon.enums.coupon.CouponTrafficPolicy
import com.coupon.enums.coupon.CouponType
import java.time.LocalDateTime

sealed class CouponCommand {
    data class Create(
        val name: String,
        val couponType: CouponType,
        val discountAmount: Long,
        val maxDiscountAmount: Long?,
        val minOrderAmount: Long?,
        val totalQuantity: Long,
        val trafficPolicy: CouponTrafficPolicy,
        val availableAt: LocalDateTime,
        val endAt: LocalDateTime,
    ) : CouponCommand()

    data class Update(
        val name: String?,
        val discountAmount: Long?,
        val maxDiscountAmount: Long?,
        val minOrderAmount: Long?,
        val trafficPolicy: CouponTrafficPolicy?,
        val availableAt: LocalDateTime?,
        val endAt: LocalDateTime?,
    ) : CouponCommand()
}
