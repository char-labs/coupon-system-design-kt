package com.coupon.auth.command

import com.coupon.auth.Token

data class GenerateTokenCommand(
    val token: Token,
)
