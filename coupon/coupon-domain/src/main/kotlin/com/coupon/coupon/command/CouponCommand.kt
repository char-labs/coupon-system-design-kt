package com.coupon.coupon.command

import com.coupon.enums.CouponType
import java.time.LocalDateTime

sealed class CouponCommand {
    data class Create(
        val name: String,
        val couponType: CouponType,
        val discountAmount: Long,
        val maxDiscountAmount: Long?,
        val minOrderAmount: Long?,
        val totalQuantity: Long,
        val availableAt: LocalDateTime,
        val endAt: LocalDateTime,
    )

    data class Update(
        val name: String?,
        val discountAmount: Long?,
        val maxDiscountAmount: Long?,
        val minOrderAmount: Long?,
        val availableAt: LocalDateTime?,
        val endAt: LocalDateTime?,
    )
}
