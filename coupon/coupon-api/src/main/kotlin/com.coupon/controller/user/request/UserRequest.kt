package com.coupon.controller.user.request

import com.coupon.user.command.UserCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 수정 요청")
sealed class UserRequest {
    data class Update(
        @Schema(description = "이름")
        val name: String?,
        @Schema(description = "이메일")
        val email: String?,
        @Schema(description = "비밀번호")
        val password: String?,
    ) {
        fun toCommand() =
            UserCommand.Update(
                name = name,
                email = email,
                password = password,
            )
    }
}
