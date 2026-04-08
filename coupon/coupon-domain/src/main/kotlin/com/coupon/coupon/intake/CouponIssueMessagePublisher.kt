package com.coupon.coupon.intake

interface CouponIssueMessagePublisher {
    fun publish(message: CouponIssueMessage): CouponIssuePublishReceipt
}
