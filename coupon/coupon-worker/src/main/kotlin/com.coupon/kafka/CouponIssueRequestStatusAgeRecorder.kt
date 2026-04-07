package com.coupon.kafka

import com.coupon.coupon.request.CouponIssueRequest
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.enums.coupon.CouponIssueRequestStatus
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Component
class CouponIssueRequestStatusAgeRecorder(
    private val couponIssueRequestService: CouponIssueRequestService,
    private val couponIssueRequestKafkaMetrics: CouponIssueRequestKafkaMetrics,
    private val clock: Clock,
) {
    /**
     * Records the oldest visible request age per status so operators can spot stuck lanes quickly.
     */
    fun recordAll() {
        record(CouponIssueRequestStatus.PENDING) { it.updatedAt ?: it.createdAt }
        record(CouponIssueRequestStatus.ENQUEUED) { it.enqueuedAt ?: it.updatedAt ?: it.createdAt }
        record(CouponIssueRequestStatus.PROCESSING) { it.processingStartedAt ?: it.updatedAt ?: it.createdAt }
    }

    private fun record(
        status: CouponIssueRequestStatus,
        timestampSelector: (CouponIssueRequest) -> LocalDateTime,
    ) {
        val oldest = couponIssueRequestService.findOldestByStatus(status)
        val age =
            oldest?.let {
                Duration.between(
                    timestampSelector(it).atZone(clock.zone).toInstant(),
                    clock.instant(),
                )
            }

        couponIssueRequestKafkaMetrics.recordOldestAge(status, age)
    }
}
