package com.coupon.coupon.request

import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import java.time.LocalDateTime

data class CouponIssueRequest(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val idempotencyKey: String,
    val status: CouponIssueRequestStatus,
    val resultCode: CouponCommandResultCode?,
    val couponIssueId: Long?,
    val failureReason: String?,
    val processedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)
