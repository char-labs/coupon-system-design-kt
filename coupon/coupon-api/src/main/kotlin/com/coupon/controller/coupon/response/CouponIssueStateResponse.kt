package com.coupon.controller.coupon.response

import com.coupon.coupon.CouponIssueStateDiagnostic

data class CouponIssueStateResponse(
    val couponId: Long,
    val initialized: Boolean,
    val occupiedCount: Long,
    val issuedCount: Long,
) {
    companion object {
        fun from(diagnostic: CouponIssueStateDiagnostic): CouponIssueStateResponse =
            CouponIssueStateResponse(
                couponId = diagnostic.couponId,
                initialized = diagnostic.initialized,
                occupiedCount = diagnostic.occupiedCount,
                issuedCount = diagnostic.issuedCount,
            )
    }
}
