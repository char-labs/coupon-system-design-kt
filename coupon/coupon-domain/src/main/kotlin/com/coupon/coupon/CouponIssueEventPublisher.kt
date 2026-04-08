package com.coupon.coupon

interface CouponIssueEventPublisher {
    fun publish(message: CouponIssueMessage): CouponIssuePublishReceipt
}
