package com.coupon.kafka

class CouponIssueRequestKafkaRetryableException(
    val requestId: Long,
    message: String,
) : RuntimeException(message)

class CouponIssueRequestKafkaDeadLetterException(
    val requestId: Long,
    message: String,
) : RuntimeException(message)

class CouponIssueRequestKafkaPayloadException(
    message: String,
) : RuntimeException(message)
