package com.coupon.user

data class UserCredential(
    val userId: Long,
    val userKey: String,
    val email: String,
    val password: String,
)
