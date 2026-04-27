package com.coupon.coupon

class CouponIssueStateNotInitializedException(
    couponId: Long,
) : RuntimeException("Coupon issue Redis state is not initialized. couponId=$couponId")
