package com.coupon.controller.user.response

import com.coupon.shared.page.Page
import com.coupon.user.UserProfile
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 페이지 응답")
data class UserPageResponse(
    @Schema(description = "사용자 목록")
    val content: List<UserResponse.Profile>,
    @Schema(description = "전체 사용자 수")
    val totalCount: Long,
) {
    companion object {
        fun from(page: Page<UserProfile>): UserPageResponse =
            UserPageResponse(
                content = page.content.map(UserResponse.Profile::from),
                totalCount = page.totalCount,
            )
    }
}
