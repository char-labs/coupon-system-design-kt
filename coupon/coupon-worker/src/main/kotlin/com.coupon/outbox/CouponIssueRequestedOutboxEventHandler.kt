package com.coupon.outbox

import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.coupon.request.CouponIssueRequestProcessingResult
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.support.outbox.OutboxEvent
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestedOutboxEventHandler(
    private val couponIssueRequestService: CouponIssueRequestService,
) : OutboxEventHandler {
    override val eventType: String = CouponOutboxEventType.COUPON_ISSUE_REQUESTED

    override fun handle(event: OutboxEvent): OutboxProcessingResult {
        val requestId =
            event.aggregateId.toLongOrNull()
                ?: return OutboxProcessingResult.Dead("Invalid coupon issue request aggregateId: ${event.aggregateId}")

        return when (val result = couponIssueRequestService.process(requestId)) {
            is CouponIssueRequestProcessingResult.Completed -> OutboxProcessingResult.Success
            is CouponIssueRequestProcessingResult.Retry -> OutboxProcessingResult.Retry(result.reason)
            is CouponIssueRequestProcessingResult.Dead -> OutboxProcessingResult.Dead(result.reason)
        }
    }
}
