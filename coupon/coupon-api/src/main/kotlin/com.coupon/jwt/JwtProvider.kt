package com.coupon.jwt

import com.coupon.auth.AuthenticationHistory
import com.coupon.auth.AuthenticationHistoryRepository
import com.coupon.auth.AuthenticationProperties
import com.coupon.auth.GrantedAuthority
import com.coupon.auth.Provider
import com.coupon.auth.RedisTokenRepository
import com.coupon.auth.Token
import com.coupon.auth.TokenRepository
import com.coupon.auth.command.AuthenticationHistoryCommand
import com.coupon.auth.command.GenerateTokenCommand
import com.coupon.enums.auth.AuthorityType
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.user.UserService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @param:Value("\${jwt.secret-key:couponJwtSecretKeyForAuthenticationSecuritySystem2024SecureKey!@#\$}")
    private val secretKey: String,
    private val authenticationProperties: AuthenticationProperties,
    private val redisTokenRepository: RedisTokenRepository,
    private val authenticationHistoryRepository: AuthenticationHistoryRepository,
    private val userService: UserService,
) : TokenRepository {
    companion object {
        private const val ISSUER = "coupon"
        private const val TOKEN_TYPE_CLAIM = "type"
        private const val ROLES_CLAIM = "roles"
        private const val ACCESS_TOKEN_TYPE = "A"
        private const val REFRESH_TOKEN_TYPE = "R"
    }

    override fun save(
        userId: Long,
        userKey: String,
    ): Token {
        // User의 실제 role을 가져와서 JWT에 포함
        val user = userService.getUser(userKey)
        val grantedAuthorities = listOf(GrantedAuthority(user.role))

        val accessToken =
            issueAccessToken(
                userKey,
                grantedAuthorities,
            )
        val refreshToken = issueRefreshToken(userKey)
        return Token(
            accessToken = accessToken,
            refreshToken = refreshToken,
        ).apply {
            redisTokenRepository.save(
                accessToken = this.accessToken,
                refreshToken = this.refreshToken,
                provider =
                    Provider(
                        userId,
                        userKey,
                        grantedAuthorities =
                            grantedAuthorities.map {
                                it.authorityType.name
                            },
                    ),
                accessTokenExpiration = authenticationProperties.accessTokenExpirationSeconds,
                refreshTokenExpiration = authenticationProperties.refreshTokenExpirationSeconds,
            )
        }
    }

    override fun renew(refreshToken: String): Token {
        validateAndParseClaims(refreshToken)
        val tokenWithAuthentication = redisTokenRepository.findByToken(refreshToken)
        val authenticationHistory =
            verifyTokenHistory(
                userKey = tokenWithAuthentication.provider.userKey,
                refreshToken = tokenWithAuthentication.refreshToken,
            )

        removeRotationToken(tokenWithAuthentication.accessToken, tokenWithAuthentication.refreshToken)

        val newAccessToken =
            issueAccessToken(
                jwtId = tokenWithAuthentication.provider.userKey,
                grantedAuthorities =
                    tokenWithAuthentication.provider.grantedAuthorities.map {
                        GrantedAuthority(AuthorityType.valueOf(it))
                    },
            )
        val newRefreshToken =
            issueRefreshToken(
                jwtId = tokenWithAuthentication.provider.userKey,
            )

        return Token(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        ).apply {
            redisTokenRepository.save(
                accessToken = this.accessToken,
                refreshToken = this.refreshToken,
                provider = tokenWithAuthentication.provider,
                accessTokenExpiration = authenticationProperties.accessTokenExpirationSeconds,
                refreshTokenExpiration = authenticationProperties.refreshTokenExpirationSeconds,
            )

            authenticationHistoryRepository.update(
                AuthenticationHistoryCommand.Update(
                    userKey = authenticationHistory.userKey,
                    refreshToken = refreshToken,
                    command =
                        GenerateTokenCommand(
                            token =
                                Token(
                                    accessToken = this.accessToken,
                                    refreshToken = this.refreshToken,
                                ),
                        ),
                ),
            )
        }
    }

    override fun remove(token: String): String {
        val claims = validateAndParseClaims(token)
        authenticationHistoryRepository.remove(token).apply {
            redisTokenRepository.deleteAllToken(this)
        }
        return claims.id
    }

    override fun removeByUserKey(userKey: String) {
        authenticationHistoryRepository.removeToken(userKey).map {
            redisTokenRepository.deleteAllToken(it)
        }
    }

    override fun findBy(accessToken: String): Provider? = redisTokenRepository.findBy(accessToken)

    /**
     * Validate JWT token and return claims
     * Public method for JwtAuthenticationFilter
     */
    fun validateToken(token: String): Claims = validateAndParseClaims(token)

    /**
     * Validate and parse JWT token
     */
    private fun validateAndParseClaims(token: String): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(getSigningKey())
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (exception: ExpiredJwtException) {
            throw ErrorException(ErrorType.INVALID_TOKEN)
        } catch (exception: Exception) {
            throw AuthenticationServiceException(exception.message, exception)
        }

    /**
     * Issue access token
     *
     * @param jwtId [String] jwt identifier.
     */
    private fun issueAccessToken(
        jwtId: String,
        grantedAuthorities: List<GrantedAuthority>,
    ): String {
        val issuedAt = Date.from(Instant.now())
        val expiresAt = Date.from(Instant.now().plusSeconds(authenticationProperties.accessTokenExpirationSeconds * 60L))

        return Jwts
            .builder()
            .issuer(ISSUER)
            .id(jwtId)
            .subject(jwtId)
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .claim(ROLES_CLAIM, grantedAuthorities.map { it.authorityType.name })
            .issuedAt(issuedAt)
            .expiration(expiresAt)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * Issue refresh token
     *
     * @param jwtId [String] jwt identifier.
     */
    private fun issueRefreshToken(jwtId: String): String {
        val issuedAt = Date.from(Instant.now())
        val expiresAt = Date.from(Instant.now().plusSeconds(authenticationProperties.refreshTokenExpirationSeconds * 60L))

        return Jwts
            .builder()
            .issuer(ISSUER)
            .id(jwtId)
            .subject(jwtId)
            .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
            .issuedAt(issuedAt)
            .expiration(expiresAt)
            .signWith(getSigningKey())
            .compact()
    }

    private fun getSigningKey(): SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))

    private fun verifyTokenHistory(
        userKey: String,
        refreshToken: String,
    ): AuthenticationHistory =
        authenticationHistoryRepository.findByUserKeyWithRefreshToken(
            userKey = userKey,
            refreshToken = refreshToken,
        )

    private fun removeRotationToken(
        accessToken: String,
        refreshToken: String,
    ) {
        redisTokenRepository.deleteToken(accessToken)
        redisTokenRepository.deleteToken(refreshToken)
    }
}
