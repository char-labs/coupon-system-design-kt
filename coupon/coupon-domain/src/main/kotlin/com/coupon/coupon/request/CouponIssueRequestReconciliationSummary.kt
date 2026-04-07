package com.coupon.coupon.request

data class CouponIssueRequestReconciliationSummary(
    val recoveredProcessingCount: Int = 0,
    val scannedPendingCount: Int = 0,
    val requeuedPendingCount: Int = 0,
    val scannedInconsistentSucceededCount: Int = 0,
    val isolatedInconsistentSucceededCount: Int = 0,
) {
    fun hasChanges(): Boolean =
        recoveredProcessingCount > 0 ||
            requeuedPendingCount > 0 ||
            isolatedInconsistentSucceededCount > 0
}
