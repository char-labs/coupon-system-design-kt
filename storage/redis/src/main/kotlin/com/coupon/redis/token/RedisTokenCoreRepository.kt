package com.coupon.redis.token

import com.coupon.auth.Provider
import com.coupon.auth.RedisTokenRepository
import com.coupon.auth.TokenWithAuthenticationResult
import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class RedisTokenCoreRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : RedisTokenRepository {
    override fun create(
        accessToken: String,
        refreshToken: String,
        provider: Provider,
        accessTokenExpiration: Long,
        refreshTokenExpiration: Long,
    ): TokenWithAuthenticationResult {
        val tokenWithAuthenticationResult =
            TokenWithAuthenticationResult(
                accessToken = accessToken,
                refreshToken = refreshToken,
                provider = provider,
            )

        redisTemplate.opsForValue().apply {
            set(
                accessToken,
                objectMapper.writeValueAsString(tokenWithAuthenticationResult),
                Duration.ofSeconds(accessTokenExpiration * 60L),
            )
            set(
                refreshToken,
                objectMapper.writeValueAsString(tokenWithAuthenticationResult),
                Duration.ofSeconds(refreshTokenExpiration * 60L),
            )
        }
        return tokenWithAuthenticationResult
    }

    override fun findByToken(token: String): TokenWithAuthenticationResult {
        redisTemplate.opsForValue().get(token).let {
            return objectMapper.readValue(it, TokenWithAuthenticationResult::class.java)
        }
    }

    override fun findBy(accessToken: String): Provider? =
        redisTemplate.opsForValue().get(accessToken)?.let {
            val result = objectMapper.readValue(it, TokenWithAuthenticationResult::class.java)
            Provider(
                userId = result.provider.userId,
                userKey = result.provider.userKey,
                grantedAuthorities = result.provider.grantedAuthorities,
            )
        }

    override fun deleteToken(token: String) {
        redisTemplate.delete(token)
    }

    override fun deleteAllToken(token: String) {
        val tokenWithAuthenticationResult =
            redisTemplate.opsForValue().get(token)?.let {
                objectMapper.readValue(it, TokenWithAuthenticationResult::class.java)
            }

        tokenWithAuthenticationResult?.accessToken?.let { redisTemplate.delete(it) }
        tokenWithAuthenticationResult?.refreshToken?.let { redisTemplate.delete(it) }
    }
}