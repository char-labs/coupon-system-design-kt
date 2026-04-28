package com.coupon.config

const val ADMIN_ONLY = "hasAuthority('ADMIN')"
const val OWNER_OR_ADMIN = "hasAuthority('ADMIN') or #p0.id == #p1"
