package com.coupon.reconciliation

import com.coupon.coupon.request.CouponIssueRequestReconciliationSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CouponIssueRequestReconciliationMetrics(
    private val meterRegistry: MeterRegistry,
) {
    fun record(
        summary: CouponIssueRequestReconciliationSummary,
        duration: Duration,
    ) {
        meterRegistry.counter("coupon.issue.request.reconciliation.run.count").increment()
        meterRegistry
            .counter("coupon.issue.request.reconciliation.processing.recovered")
            .increment(summary.recoveredProcessingCount.toDouble())
        meterRegistry
            .counter("coupon.issue.request.reconciliation.pending.scanned")
            .increment(summary.scannedPendingCount.toDouble())
        meterRegistry
            .counter("coupon.issue.request.reconciliation.pending.requeued")
            .increment(summary.requeuedPendingCount.toDouble())
        meterRegistry
            .counter("coupon.issue.request.reconciliation.succeeded.inconsistent.scanned")
            .increment(summary.scannedInconsistentSucceededCount.toDouble())
        meterRegistry
            .counter("coupon.issue.request.reconciliation.succeeded.inconsistent.isolated")
            .increment(summary.isolatedInconsistentSucceededCount.toDouble())

        Timer
            .builder("coupon.issue.request.reconciliation.duration")
            .register(meterRegistry)
            .record(duration)
    }
}
