package com.coupon.coupon.request

data class CouponIssueRequestedMessage(
    val requestId: Long,
    val couponId: Long,
    val userId: Long,
    val idempotencyKey: String,
) {
    companion object {
        fun from(request: CouponIssueRequest) =
            CouponIssueRequestedMessage(
                requestId = request.id,
                couponId = request.couponId,
                userId = request.userId,
                idempotencyKey = request.idempotencyKey,
            )
    }
}
