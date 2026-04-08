package com.coupon.coupon.restaurant.criteria

import com.coupon.coupon.restaurant.command.RestaurantCouponCommand
import com.coupon.enums.coupon.RestaurantCouponStatus
import java.time.LocalDateTime

sealed class RestaurantCouponCriteria {
    data class Create(
        val restaurantId: Long,
        val couponId: Long,
        val status: RestaurantCouponStatus,
        val availableAt: LocalDateTime,
        val endAt: LocalDateTime,
    ) {
        companion object {
            fun of(command: RestaurantCouponCommand.Create) =
                Create(
                    restaurantId = command.restaurantId,
                    couponId = command.couponId,
                    status = RestaurantCouponStatus.ACTIVE,
                    availableAt = command.availableAt,
                    endAt = command.endAt,
                )
        }
    }
}
