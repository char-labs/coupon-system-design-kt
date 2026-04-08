package com.coupon.storage.rdb.coupon.restaurant

import com.coupon.enums.coupon.RestaurantCouponStatus
import org.springframework.data.jpa.repository.JpaRepository

interface RestaurantCouponJpaRepository : JpaRepository<RestaurantCouponEntity, Long> {
    fun existsByRestaurantIdAndCouponId(
        restaurantId: Long,
        couponId: Long,
    ): Boolean

    fun findByRestaurantIdAndStatusAndDeletedAtIsNull(
        restaurantId: Long,
        status: RestaurantCouponStatus,
    ): RestaurantCouponEntity?

    fun findAllByStatusAndDeletedAtIsNull(status: RestaurantCouponStatus): List<RestaurantCouponEntity>
}
