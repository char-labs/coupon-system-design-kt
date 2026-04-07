package com.coupon.coupon

import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CouponValidator(
    private val couponRepository: CouponRepository,
) {
    fun validateAvailability(couponId: Long) {
        val coupon = couponRepository.findDetailById(couponId)

        validateStatus(coupon)
        validateIssuablePeriod(coupon)
    }

    private fun validateStatus(coupon: CouponDetail) {
        if (!coupon.isActive()) {
            throw ErrorException(ErrorType.COUPON_NOT_ACTIVE)
        }
    }

    private fun validateIssuablePeriod(coupon: CouponDetail) {
        val now = LocalDateTime.now()
        if (now.isBefore(coupon.availableAt) || now.isAfter(coupon.endAt)) {
            throw ErrorException(ErrorType.COUPON_EXPIRED)
        }
    }
}
