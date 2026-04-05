package com.coupon.coupon

import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import org.springframework.stereotype.Component

@Component
class CouponIssueValidator(
    private val couponIssueRepository: CouponIssueRepository,
    private val couponValidator: CouponValidator,
) {
    fun validateIssuable(
        userId: Long,
        couponId: Long,
    ) {
        validateNotAlreadyIssued(userId, couponId)
        couponValidator.validateAvailability(couponId)
    }

    fun validateOwnedCouponIssue(
        couponIssue: CouponIssue,
        userId: Long,
    ) {
        validateOwner(couponIssue, userId)
    }

    private fun validateNotAlreadyIssued(
        userId: Long,
        couponId: Long,
    ) {
        if (couponIssueRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw ErrorException(ErrorType.ALREADY_ISSUED_COUPON)
        }
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
