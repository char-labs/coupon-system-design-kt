package com.coupon.coupon.request

sealed interface CouponIssueRequestProcessingResult {
    data class Completed(
        val request: CouponIssueRequest,
        val transitioned: Boolean = true,
    ) : CouponIssueRequestProcessingResult

    data class Retry(
        val reason: String,
        val request: CouponIssueRequest? = null,
        val transitioned: Boolean = false,
    ) : CouponIssueRequestProcessingResult

    data class Dead(
        val reason: String,
        val request: CouponIssueRequest? = null,
    ) : CouponIssueRequestProcessingResult
}
