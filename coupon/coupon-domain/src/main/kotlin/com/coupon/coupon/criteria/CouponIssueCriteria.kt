package com.coupon.coupon.criteria

import com.coupon.coupon.command.CouponIssueCommand

sealed class CouponIssueCriteria {
    data class Create(
        val couponId: Long,
        val userId: Long,
    ) {
        companion object {
            fun of(command: CouponIssueCommand.Issue) =
                Create(
                    couponId = command.couponId,
                    userId = command.userId,
                )
        }
    }
}
