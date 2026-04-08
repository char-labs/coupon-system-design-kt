package com.coupon.controller.auth

import com.coupon.auth.AuthFacade
import com.coupon.common.ApiResponse
import com.coupon.controller.auth.request.AuthRequest
import com.coupon.controller.auth.response.TokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth API", description = "인증 관련 API")
@RestController
class AuthController(
    private val authFacade: AuthFacade,
) {
    @Operation(summary = "이메일 회원가입", description = "이메일로 회원가입을 합니다.")
    @PostMapping("/signup")
    fun signup(
        @RequestBody @Valid request: AuthRequest.Signup,
    ): ApiResponse<TokenResponse> {
        val token = authFacade.signup(request.toCommand())

        return ApiResponse.success(
            HttpStatus.CREATED.value(),
            TokenResponse.from(token),
        )
    }

    @Operation(summary = "로그인", description = "로그인을 합니다.")
    @PostMapping("/signin")
    fun signin(
        @RequestBody @Valid request: AuthRequest.Signin,
    ): TokenResponse = TokenResponse.from(authFacade.signin(request.toCommand()))
}
