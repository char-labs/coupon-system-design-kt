package com.coupon.coupon.execution

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.CouponService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.shared.lock.WithDistributedLock
import org.springframework.stereotype.Service

@Service
class CouponIssueLockingExecutor(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
) {
    @WithDistributedLock(
        key = "'COUPON_ISSUE:' + #command.couponId",
        requiresNew = true,
    )
    fun executeIssue(command: CouponIssueCommand.Issue): CouponIssue {
        couponService.validateAvailability(command.couponId)
        val decreased = couponService.decreaseQuantityIfAvailable(command.couponId)
        if (!decreased) {
            throw ErrorException(ErrorType.COUPON_OUT_OF_STOCK)
        }

        return couponIssueService.executeIssue(command)
    }
}
