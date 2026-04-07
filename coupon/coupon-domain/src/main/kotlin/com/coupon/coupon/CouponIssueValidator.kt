package com.coupon.coupon

import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import org.springframework.stereotype.Component

@Component
class CouponIssueValidator {
    fun validateOwnedCouponIssue(
        couponIssue: CouponIssue,
        userId: Long,
    ) {
        validateOwner(couponIssue, userId)
    }

    private fun validateOwner(
        couponIssue: CouponIssue,
        userId: Long,
    ) {
        if (couponIssue.userId != userId) {
            throw ErrorException(ErrorType.FORBIDDEN_COUPON_ISSUE)
        }
    }
}
