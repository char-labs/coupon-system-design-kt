package com.coupon.coupon.request

sealed interface CouponIssueRequestProcessingResult {
    data class Completed(
        val request: CouponIssueRequest,
    ) : CouponIssueRequestProcessingResult

    data class Retry(
        val reason: String,
    ) : CouponIssueRequestProcessingResult

    data class Dead(
        val reason: String,
    ) : CouponIssueRequestProcessingResult
}
