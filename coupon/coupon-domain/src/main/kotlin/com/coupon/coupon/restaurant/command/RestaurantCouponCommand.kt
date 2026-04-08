package com.coupon.coupon.restaurant.command

import java.time.LocalDateTime

sealed class RestaurantCouponCommand {
    data class Create(
        val restaurantId: Long,
        val couponId: Long,
        val availableAt: LocalDateTime,
        val endAt: LocalDateTime,
    ) : RestaurantCouponCommand()

    data class CreateBatch(
        val items: List<Create>,
    ) : RestaurantCouponCommand()
}
