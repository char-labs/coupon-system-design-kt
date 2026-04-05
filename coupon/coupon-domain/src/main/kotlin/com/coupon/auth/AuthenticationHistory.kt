package com.coupon.auth

import com.coupon.enums.auth.TokenStatus
import java.time.LocalDateTime

data class AuthenticationHistory(
    val authenticationId: Long,
    val userKey: String,
    val token: Token,
    val status: TokenStatus,
    val loggedInAt: LocalDateTime,
)
