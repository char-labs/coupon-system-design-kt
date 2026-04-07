package com.coupon.controller.coupon.request

import com.coupon.coupon.request.command.CouponIssueRequestCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "쿠폰 발급 요청 접수")
data class CouponIssueRequestMessage(
    @param:Schema(description = "쿠폰 ID", example = "1")
    val couponId: Long,
) {
    fun toCommand(userId: Long) =
        CouponIssueRequestCommand.Accept(
            couponId = couponId,
            userId = userId,
        )
}
