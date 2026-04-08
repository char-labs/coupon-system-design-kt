package com.coupon.coupon.restaurant

import com.coupon.coupon.restaurant.criteria.RestaurantCouponCriteria

interface RestaurantCouponRepository {
    fun save(criteria: RestaurantCouponCriteria.Create): RestaurantCoupon

    fun existsByRestaurantIdAndCouponId(
        restaurantId: Long,
        couponId: Long,
    ): Boolean

    fun findActiveByRestaurantId(restaurantId: Long): RestaurantCoupon

    fun findAllActive(): List<RestaurantCoupon>

    fun findById(id: Long): RestaurantCoupon

    fun delete(id: Long)
}
