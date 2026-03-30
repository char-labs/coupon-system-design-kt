package com.coupon.controller.coupon.response

import com.coupon.coupon.CouponIssue
import com.coupon.support.page.Page
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "쿠폰 발급 페이지 응답")
data class CouponIssuePageResponse(
    @param:Schema(description = "쿠폰 발급 목록")
    val content: List<CouponIssueResponse.Detail>,
    @param:Schema(description = "전체 쿠폰 발급 수", example = "50")
    val totalCount: Long,
) {
    companion object {
        fun from(page: Page<CouponIssue.Detail>): CouponIssuePageResponse =
            CouponIssuePageResponse(
                content = page.content.map(CouponIssueResponse.Detail::from),
                totalCount = page.totalCount,
            )
    }
}
