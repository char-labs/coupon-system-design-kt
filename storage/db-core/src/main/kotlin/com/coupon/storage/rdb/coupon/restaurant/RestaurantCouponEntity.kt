package com.coupon.storage.rdb.coupon.restaurant

import com.coupon.coupon.restaurant.RestaurantCoupon
import com.coupon.coupon.restaurant.criteria.RestaurantCouponCriteria
import com.coupon.enums.coupon.RestaurantCouponStatus
import com.coupon.storage.rdb.support.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "t_restaurant_coupon",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_restaurant_coupon_rid_cid",
            columnNames = ["restaurant_id", "coupon_id"],
        ),
    ],
)
class RestaurantCouponEntity(
    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: Long,
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)", nullable = false)
    var status: RestaurantCouponStatus,
    @Column(name = "available_at", nullable = false)
    val availableAt: LocalDateTime,
    @Column(name = "end_at", nullable = false)
    val endAt: LocalDateTime,
) : BaseEntity() {
    constructor(criteria: RestaurantCouponCriteria.Create) : this(
        restaurantId = criteria.restaurantId,
        couponId = criteria.couponId,
        status = criteria.status,
        availableAt = criteria.availableAt,
        endAt = criteria.endAt,
    )

    fun toRestaurantCoupon() =
        RestaurantCoupon(
            id = id!!,
            restaurantId = restaurantId,
            couponId = couponId,
            status = status,
            availableAt = availableAt,
            endAt = endAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
