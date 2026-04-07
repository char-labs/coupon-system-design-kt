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
    val enqueuedAt: LocalDateTime?,
    val processingStartedAt: LocalDateTime?,
    val deliveryAttemptCount: Int,
    val lastDeliveryError: String?,
    val processedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
) {
    fun isTerminal(): Boolean = status.isTerminal()

    fun hasLeftPending(): Boolean = status.hasLeftPending()
}
