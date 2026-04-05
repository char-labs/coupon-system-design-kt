package com.coupon.coupon

import com.coupon.enums.coupon.CouponPreviewInapplicableReason
import com.coupon.enums.coupon.CouponType

data class CouponPreview(
    val couponId: Long,
    val couponCode: String,
    val couponName: String,
    val couponType: CouponType,
    val orderAmount: Long,
    val applicable: Boolean,
    val discountAmount: Long,
    val reason: CouponPreviewInapplicableReason?,
)
