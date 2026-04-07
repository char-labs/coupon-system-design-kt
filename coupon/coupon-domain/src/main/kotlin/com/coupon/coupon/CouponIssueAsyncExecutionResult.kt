package com.coupon.coupon

import com.coupon.enums.error.ErrorType

sealed interface CouponIssueAsyncExecutionResult {
    data class Succeeded(
        val couponIssueId: Long,
    ) : CouponIssueAsyncExecutionResult

    data object AlreadyIssued : CouponIssueAsyncExecutionResult

    data class Rejected(
        val errorType: ErrorType,
    ) : CouponIssueAsyncExecutionResult

    data class Retry(
        val reason: String,
    ) : CouponIssueAsyncExecutionResult
}
