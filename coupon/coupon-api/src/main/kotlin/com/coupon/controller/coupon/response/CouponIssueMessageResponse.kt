package com.coupon.controller.coupon.response

import com.coupon.enums.coupon.CouponIssueResult
import io.swagger.v3.oas.annotations.media.Schema

data class CouponIssueMessageResponse(
    @param:Schema(description = "쿠폰 발급 즉시 판정 결과", example = "SUCCESS")
    val result: CouponIssueResult,
    @param:Schema(
        description = "쿠폰 발급 즉시 판정 메시지",
        example = "쿠폰 발급 요청이 성공적으로 접수되었습니다. 잠시 후 쿠폰함에서 확인해주세요.",
    )
    val message: String,
) {
    companion object {
        fun of(result: CouponIssueResult) = CouponIssueMessageResponse(result = result, message = result.description)
    }
}
