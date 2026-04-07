package com.coupon.controller.coupon.response

import io.swagger.v3.oas.annotations.media.Schema

data class CouponIssueMessageResponse(
    @param:Schema(description = "쿠폰 발급 메시지", example = "쿠폰이 성공적으로 발급되었습니다.")
    val message: String,
) {
    companion object {
        fun of(message: String) = CouponIssueMessageResponse(message)
    }
}
