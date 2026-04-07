package com.coupon.coupon.request.command

sealed interface CouponIssueRequestCommand {
    data class Accept(
        val couponId: Long,
        val userId: Long,
    ) : CouponIssueRequestCommand {
        val idempotencyKey: String
            get() = "coupon:$couponId:user:$userId:action:ISSUE"
    }
}
