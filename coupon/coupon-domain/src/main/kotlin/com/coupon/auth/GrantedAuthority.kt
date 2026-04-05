package com.coupon.auth

import com.coupon.enums.auth.AuthorityType

data class GrantedAuthority(
    val authorityType: AuthorityType,
)
