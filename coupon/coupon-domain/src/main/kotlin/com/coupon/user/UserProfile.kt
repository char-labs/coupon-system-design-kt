package com.coupon.user

import com.coupon.enums.AuthorityType
import java.time.LocalDateTime

data class UserProfile(
    val id: Long,
    val key: String,
    val name: String,
    val email: String,
    val role: AuthorityType,
    val createdAt: LocalDateTime,
)
