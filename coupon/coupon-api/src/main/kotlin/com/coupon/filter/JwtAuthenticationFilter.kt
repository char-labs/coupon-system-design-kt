package com.coupon.filter

import com.coupon.auth.Provider
import com.coupon.jwt.JwtProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)

        if (token != null) {
            try {
                val provider = jwtProvider.validateAccessToken(token)
                setAuthentication(provider)
            } catch (e: Exception) {
                // 토큰이 유효하지 않으면 인증 없이 진행 (Security에서 처리)
                logger.debug("Invalid JWT token: ${e.message}")
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    private fun setAuthentication(provider: Provider) {
        val authorities = provider.grantedAuthorities.map { SimpleGrantedAuthority(it) }

        val authentication =
            UsernamePasswordAuthenticationToken(
                provider.userKey,
                null,
                authorities,
            )

        SecurityContextHolder.getContext().authentication = authentication
    }
}
