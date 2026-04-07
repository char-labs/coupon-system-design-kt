package com.coupon.coupon.event

object CouponOutboxEventType {
    const val COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED"
    const val COUPON_ISSUED = "COUPON_ISSUED"
    const val COUPON_USED = "COUPON_USED"
    const val COUPON_CANCELED = "COUPON_CANCELED"
}
