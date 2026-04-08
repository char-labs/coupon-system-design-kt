package com.coupon.coupon.fixture

import com.coupon.coupon.restaurant.RestaurantCoupon
import com.coupon.coupon.restaurant.command.RestaurantCouponCommand
import com.coupon.enums.coupon.RestaurantCouponStatus
import java.time.LocalDateTime

internal object RestaurantCouponFixtures {
    fun createCommand(
        restaurantId: Long = 101L,
        couponId: Long = 2001L,
        referenceTime: LocalDateTime = LocalDateTime.now(),
        availableAt: LocalDateTime = referenceTime.minusHours(1),
        endAt: LocalDateTime = referenceTime.plusHours(1),
    ) = RestaurantCouponCommand.Create(
        restaurantId = restaurantId,
        couponId = couponId,
        availableAt = availableAt,
        endAt = endAt,
    )

    fun createBatch(vararg items: RestaurantCouponCommand.Create) =
        RestaurantCouponCommand.CreateBatch(
            items = items.toList(),
        )

    fun coupon(
        id: Long = 1L,
        restaurantId: Long = 101L,
        couponId: Long = 2001L,
        status: RestaurantCouponStatus = RestaurantCouponStatus.ACTIVE,
        referenceTime: LocalDateTime = LocalDateTime.now(),
        availableAt: LocalDateTime = referenceTime.minusHours(1),
        endAt: LocalDateTime = referenceTime.plusHours(1),
        createdAt: LocalDateTime = referenceTime.minusDays(1),
        updatedAt: LocalDateTime? = referenceTime.minusHours(1),
    ) = RestaurantCoupon(
        id = id,
        restaurantId = restaurantId,
        couponId = couponId,
        status = status,
        availableAt = availableAt,
        endAt = endAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
