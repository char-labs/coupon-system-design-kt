package com.coupon.coupon.criteria

import com.coupon.coupon.command.CouponCommand
import com.coupon.enums.CouponType
import java.time.LocalDateTime

sealed class CouponCriteria {
    data class Create(
        val couponCode: String,
        val name: String,
        val couponType: CouponType,
        val discountAmount: Long,
        val maxDiscountAmount: Long?,
        val minOrderAmount: Long?,
        val totalQuantity: Long,
        val availableAt: LocalDateTime,
        val endAt: LocalDateTime,
    ) {
        companion object {
            fun of(
                couponCode: String,
                command: CouponCommand.Create,
            ) = Create(
                couponCode = couponCode,
                name = command.name,
                couponType = command.couponType,
                discountAmount = command.discountAmount,
                maxDiscountAmount = command.maxDiscountAmount,
                minOrderAmount = command.minOrderAmount,
                totalQuantity = command.totalQuantity,
                availableAt = command.availableAt,
                endAt = command.endAt,
            )
        }
    }

    data class Update(
        val name: String?,
        val discountAmount: Long?,
        val maxDiscountAmount: Long?,
        val minOrderAmount: Long?,
        val availableAt: LocalDateTime?,
        val endAt: LocalDateTime?,
    ) {
        companion object {
            fun of(command: CouponCommand.Update) =
                Update(
                    name = command.name,
                    discountAmount = command.discountAmount,
                    maxDiscountAmount = command.maxDiscountAmount,
                    minOrderAmount = command.minOrderAmount,
                    availableAt = command.availableAt,
                    endAt = command.endAt,
                )
        }
    }
}
