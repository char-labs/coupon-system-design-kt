package com.coupon.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt-token")
data class AuthenticationProperties(
    val accessTokenExpirationSeconds: Long,
    val refreshTokenExpirationSeconds: Long,
) {
    init {
        require(accessTokenExpirationSeconds > 0) { "jwt-token.access-token-expiration-seconds must be greater than 0" }
        require(refreshTokenExpirationSeconds > 0) { "jwt-token.refresh-token-expiration-seconds must be greater than 0" }
    }
}
