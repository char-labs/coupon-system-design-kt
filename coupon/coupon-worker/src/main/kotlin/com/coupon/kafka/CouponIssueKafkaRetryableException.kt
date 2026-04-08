package com.coupon.kafka

class CouponIssueKafkaRetryableException(
    val couponId: Long,
    val userId: Long,
    message: String,
) : RuntimeException(message)
