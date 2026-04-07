package com.coupon.reconciliation

import com.coupon.config.CouponIssueRequestReconciliationProperties
import com.coupon.coupon.request.CouponIssueRequestReconciliationService
import com.coupon.kafka.CouponIssueRequestStatusAgeRecorder
import com.coupon.support.logging.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(
    prefix = "worker.coupon-issue-request-reconciliation",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CouponIssueRequestReconciliationPoller(
    private val couponIssueRequestReconciliationProperties: CouponIssueRequestReconciliationProperties,
    private val couponIssueRequestReconciliationService: CouponIssueRequestReconciliationService,
    private val couponIssueRequestReconciliationMetrics: CouponIssueRequestReconciliationMetrics,
    private val couponIssueRequestStatusAgeRecorder: CouponIssueRequestStatusAgeRecorder,
    private val clock: Clock,
) {
    private val log by logger()

    @Scheduled(
        fixedDelayString = "\${worker.coupon-issue-request-reconciliation.fixed-delay}",
        initialDelayString = "\${worker.coupon-issue-request-reconciliation.initial-delay}",
    )
    fun reconcile() {
        val startedAt = clock.instant()

        val summary =
            couponIssueRequestReconciliationService.reconcile(
                processingUpdatedBefore = now().minus(couponIssueRequestReconciliationProperties.processingTimeout),
                pendingUpdatedBefore = now().minus(couponIssueRequestReconciliationProperties.pendingTimeout),
                batchSize = couponIssueRequestReconciliationProperties.batchSize,
            )

        couponIssueRequestReconciliationMetrics.record(
            summary = summary,
            duration = Duration.between(startedAt, clock.instant()),
        )
        couponIssueRequestStatusAgeRecorder.recordAll()

        if (summary.hasChanges()) {
            log.warn {
                "Coupon issue request reconciliation recovered=${summary.recoveredProcessingCount}, " +
                    "requeued=${summary.requeuedPendingCount}, " +
                    "isolated=${summary.isolatedInconsistentSucceededCount}"
            }
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), clock.zone)
}
