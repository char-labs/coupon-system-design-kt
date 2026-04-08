package com.coupon.storage.rdb.coupon.restaurant

import com.coupon.coupon.restaurant.RestaurantCoupon
import com.coupon.coupon.restaurant.RestaurantCouponRepository
import com.coupon.coupon.restaurant.criteria.RestaurantCouponCriteria
import com.coupon.enums.coupon.RestaurantCouponStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.storage.rdb.support.findByIdOrElseThrow
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class RestaurantCouponCoreRepository(
    private val restaurantCouponJpaRepository: RestaurantCouponJpaRepository,
) : RestaurantCouponRepository {
    override fun save(criteria: RestaurantCouponCriteria.Create): RestaurantCoupon =
        try {
            restaurantCouponJpaRepository
                .save(RestaurantCouponEntity(criteria))
                .toRestaurantCoupon()
        } catch (e: DataIntegrityViolationException) {
            throw ErrorException(ErrorType.DUPLICATED_RESTAURANT_COUPON)
        }

    override fun findActiveByRestaurantId(restaurantId: Long): RestaurantCoupon =
        restaurantCouponJpaRepository
            .findByRestaurantIdAndStatusAndDeletedAtIsNull(restaurantId, RestaurantCouponStatus.ACTIVE)
            ?.toRestaurantCoupon()
            ?: throw ErrorException(ErrorType.NOT_FOUND_RESTAURANT_COUPON)

    override fun findAllActive(): List<RestaurantCoupon> =
        restaurantCouponJpaRepository
            .findAllByStatusAndDeletedAtIsNull(RestaurantCouponStatus.ACTIVE)
            .map { it.toRestaurantCoupon() }

    override fun findById(id: Long): RestaurantCoupon = restaurantCouponJpaRepository.findByIdOrElseThrow(id).toRestaurantCoupon()

    override fun delete(id: Long) {
        val entity = restaurantCouponJpaRepository.findByIdOrElseThrow(id)
        entity.softDelete()
    }
}
