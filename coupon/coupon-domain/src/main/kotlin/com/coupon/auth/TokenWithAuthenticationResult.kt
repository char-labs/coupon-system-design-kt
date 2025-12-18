package com.coupon.auth

data class TokenWithAuthenticationResult(
    val accessToken: String,
    val refreshToken: String,
    val provider: Provider,
)
