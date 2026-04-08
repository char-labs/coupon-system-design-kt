package com.coupon.controller.coupon.request

import com.coupon.coupon.command.CouponIssueCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "쿠폰 발급 요청")
data class CouponIssueRequest(
    @param:Schema(description = "쿠폰 ID", example = "1")
    val couponId: Long,
) {
    fun toCommand(userId: Long) =
        CouponIssueCommand.Issue(
            couponId = couponId,
            userId = userId,
        )
}
