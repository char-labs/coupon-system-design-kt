package com.coupon.auth

interface TokenRepository {
    fun save(
        userId: Long,
        userKey: String,
    ): Token

    fun renew(refreshToken: String): Token

    fun remove(token: String): String

    fun removeByUserKey(userKey: String)

    fun findBy(accessToken: String): Provider?
}
