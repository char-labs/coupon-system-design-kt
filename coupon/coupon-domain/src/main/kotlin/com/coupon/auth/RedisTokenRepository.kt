package com.coupon.auth

interface RedisTokenRepository {
    fun save(
        accessToken: String,
        refreshToken: String,
        provider: Provider,
        accessTokenExpiration: Long,
        refreshTokenExpiration: Long,
    ): TokenWithAuthenticationResult

    fun findByToken(token: String): TokenWithAuthenticationResult

    fun findBy(accessToken: String): Provider?

    fun deleteToken(token: String)

    fun deleteAllToken(token: String)
}
