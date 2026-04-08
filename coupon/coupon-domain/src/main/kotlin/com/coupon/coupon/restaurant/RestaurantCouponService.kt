package com.coupon.coupon.restaurant

import com.coupon.coupon.CouponRepository
import com.coupon.coupon.restaurant.command.RestaurantCouponCommand
import com.coupon.coupon.restaurant.criteria.RestaurantCouponCriteria
import com.coupon.enums.coupon.RestaurantCouponStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RestaurantCouponService(
    private val restaurantCouponRepository: RestaurantCouponRepository,
    private val couponRepository: CouponRepository,
) {
    companion object {
        private const val MAX_BATCH_SIZE = 3
    }

    fun createRestaurantCoupon(command: RestaurantCouponCommand.Create): RestaurantCoupon {
        couponRepository.findById(command.couponId)
        return restaurantCouponRepository.save(RestaurantCouponCriteria.Create.of(command))
    }

    fun createRestaurantCoupons(command: RestaurantCouponCommand.CreateBatch): List<RestaurantCoupon> {
        if (command.items.size !in 1..MAX_BATCH_SIZE) {
            throw ErrorException(ErrorType.INVALID_RESTAURANT_COUPON_BATCH_SIZE)
        }

        return command.items.map { createRestaurantCoupon(it) }
    }

    fun getActiveRestaurantCoupon(restaurantId: Long): RestaurantCoupon {
        val restaurantCoupon = restaurantCouponRepository.findActiveByRestaurantId(restaurantId)
        validateActive(restaurantCoupon)
        return restaurantCoupon
    }

    fun getActiveRestaurantCoupons(): List<RestaurantCoupon> = restaurantCouponRepository.findAllActive()

    fun deleteRestaurantCoupon(id: Long) {
        restaurantCouponRepository.delete(id)
    }

    private fun validateActive(restaurantCoupon: RestaurantCoupon) {
        if (restaurantCoupon.status != RestaurantCouponStatus.ACTIVE) {
            throw ErrorException(ErrorType.RESTAURANT_COUPON_NOT_ACTIVE)
        }
        val now = LocalDateTime.now()
        if (now.isBefore(restaurantCoupon.availableAt) || now.isAfter(restaurantCoupon.endAt)) {
            throw ErrorException(ErrorType.RESTAURANT_COUPON_EXPIRED)
        }
    }
}
