package com.coupon.controller.auth.response

import com.coupon.auth.Token
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 응답")
data class TokenResponse(
    @Schema(description = "액세스 토큰")
    val accessToken: String,
    @Schema(description = "리프레시 토큰")
    val refreshToken: String,
) {
    companion object {
        fun from(token: Token) =
            TokenResponse(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
            )
    }
}
