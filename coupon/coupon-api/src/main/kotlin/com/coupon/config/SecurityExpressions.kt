package com.coupon.config

// Temporary authority expression for load testing. Replace admin APIs with ADMIN_ONLY after the load test window.
const val LOAD_TEST_ADMIN_OR_USER = "hasAnyAuthority('ADMIN', 'USER')"
const val ADMIN_ONLY = "hasAuthority('ADMIN')"
const val OWNER_OR_ADMIN = "hasAuthority('ADMIN') or #p0.id == #p1"
