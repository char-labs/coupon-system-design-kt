package com.coupon.coupon.restaurant

import com.coupon.enums.coupon.RestaurantCouponStatus
import java.time.LocalDateTime

data class RestaurantCoupon(
    val id: Long,
    val restaurantId: Long,
    val couponId: Long,
    val status: RestaurantCouponStatus,
    val availableAt: LocalDateTime,
    val endAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)
