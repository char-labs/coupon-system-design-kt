package com.coupon.coupon

import com.coupon.enums.CouponStatus
import com.coupon.enums.CouponType
import java.time.LocalDateTime

data class CouponDetail(
    val id: Long,
    val code: String,
    val name: String,
    val type: CouponType,
    val status: CouponStatus,
    val discountAmount: Long,
    val maxDiscountAmount: Long?,
    val minOrderAmount: Long?,
    val totalQuantity: Long,
    val remainingQuantity: Long,
    val availableAt: LocalDateTime,
    val endAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)
