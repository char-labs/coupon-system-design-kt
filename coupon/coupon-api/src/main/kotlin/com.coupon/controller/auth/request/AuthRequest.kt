package com.coupon.controller.auth.request

import com.coupon.auth.command.AuthCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "인증 요청")
sealed class AuthRequest {
    data class Signup(
        @Schema(description = "이름")
        @field:NotBlank
        val name: String,
        @Schema(description = "이메일")
        @field:NotBlank
        @field:Email
        val email: String,
        @Schema(description = "비밀번호")
        @field:NotBlank
        val password: String,
    ) {
        fun toCommand() =
            AuthCommand.SignUp(
                name = name,
                email = email,
                password = password,
            )
    }

    data class Signin(
        @Schema(description = "이메일")
        @field:NotBlank
        @field:Email
        val email: String,
        @Schema(description = "비밀번호")
        @field:NotBlank
        val password: String,
    ) {
        fun toCommand() =
            AuthCommand.SignIn(
                email = email,
                password = password,
            )
    }
}
