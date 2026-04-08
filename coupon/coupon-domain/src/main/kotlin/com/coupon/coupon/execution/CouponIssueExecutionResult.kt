package com.coupon.coupon.execution

import com.coupon.enums.error.ErrorType

sealed interface CouponIssueExecutionResult {
    data class Succeeded(
        val couponIssueId: Long,
    ) : CouponIssueExecutionResult

    data object AlreadyIssued : CouponIssueExecutionResult

    data class Rejected(
        val errorType: ErrorType,
    ) : CouponIssueExecutionResult

    data class Retry(
        val reason: String,
    ) : CouponIssueExecutionResult
}
