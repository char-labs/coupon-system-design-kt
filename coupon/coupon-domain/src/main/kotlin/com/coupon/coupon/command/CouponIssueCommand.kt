package com.coupon.coupon.command

sealed class CouponIssueCommand {
    data class Issue(
        val couponId: Long,
        val userId: Long,
    )

    data class Use(
        val couponIssueId: Long,
        val userId: Long,
    )

    data class Cancel(
        val couponIssueId: Long,
        val userId: Long,
    )
}
