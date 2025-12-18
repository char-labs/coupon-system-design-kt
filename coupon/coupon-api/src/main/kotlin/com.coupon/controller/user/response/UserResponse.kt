package com.coupon.controller.user.response

import com.coupon.enums.AuthorityType
import com.coupon.user.UserProfile
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사용자 응답")
sealed class UserResponse {
    data class Profile(
        @Schema(description = "사용자 ID")
        val id: Long,
        @Schema(description = "사용자 키")
        val key: String,
        @Schema(description = "사용자 이름")
        val name: String,
        @Schema(description = "사용자 이메일")
        val email: String,
        @Schema(description = "사용자 권한")
        val role: AuthorityType,
        @Schema(description = "사용자 생성일")
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun from(profile: UserProfile) =
                Profile(
                    id = profile.id,
                    key = profile.key,
                    name = profile.name,
                    email = profile.email,
                    role = profile.role,
                    createdAt = profile.createdAt,
                )
        }
    }
}
