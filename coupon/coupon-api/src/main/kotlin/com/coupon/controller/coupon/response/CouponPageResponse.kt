package com.coupon.controller.coupon.response

import com.coupon.coupon.CouponDetail
import com.coupon.shared.page.Page
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "쿠폰 페이지 응답")
data class CouponPageResponse(
    @param:Schema(description = "쿠폰 목록")
    val content: List<CouponResponse.Detail>,
    @param:Schema(description = "전체 쿠폰 수", example = "100")
    val totalCount: Long,
) {
    companion object {
        fun from(page: Page<CouponDetail>): CouponPageResponse =
            CouponPageResponse(
                content = page.content.map(CouponResponse.Detail::from),
                totalCount = page.totalCount,
            )
    }
}
