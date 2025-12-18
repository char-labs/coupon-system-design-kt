package com.coupon.auth

data class Provider(
    val userId: Long,
    val userKey: String,
    val grantedAuthorities: List<String>,
)
