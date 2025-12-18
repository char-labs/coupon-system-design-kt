package com.coupon.user

import com.coupon.enums.AuthorityType

data class User(
    val id: Long,
    val key: String,
    val role: AuthorityType,
)
