package com.coupon.coupon.intake

import com.coupon.enums.coupon.CouponIssueResult
import java.time.Duration

interface CouponIssueFlowMetrics {
    fun recordIntakeReserve(
        result: CouponIssueResult,
        duration: Duration,
    ) = Unit

    fun recordIntakePublish(
        result: String,
        duration: Duration,
    ) = Unit

    fun recordIntakeCompensation(result: String) = Unit

    fun recordWorkerLimit(duration: Duration) = Unit

    fun recordWorkerConsume(
        result: String,
        duration: Duration,
    ) = Unit

    fun recordWorkerDlq(
        result: String,
        duration: Duration,
    ) = Unit
}
