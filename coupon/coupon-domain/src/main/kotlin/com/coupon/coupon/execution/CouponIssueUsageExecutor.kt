package com.coupon.coupon.execution

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.shared.lock.WithDistributedLock
import org.springframework.stereotype.Service

@Service
class CouponIssueUsageExecutor(
    private val couponIssueService: CouponIssueService,
) {
    @WithDistributedLock(
        key = "'COUPON_ISSUE_STATUS:' + #command.couponIssueId",
        requiresNew = true,
    )
    fun useCoupon(command: CouponIssueCommand.Use): CouponIssue.Detail = couponIssueService.useCoupon(command)
}
