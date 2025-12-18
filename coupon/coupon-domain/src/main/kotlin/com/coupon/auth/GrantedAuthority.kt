package com.coupon.auth

import com.coupon.enums.AuthorityType

data class GrantedAuthority(
    val authorityType: AuthorityType,
)
