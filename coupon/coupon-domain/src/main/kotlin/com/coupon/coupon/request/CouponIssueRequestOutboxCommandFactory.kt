package com.coupon.coupon.request

import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.support.outbox.command.OutboxEventCommand

internal object CouponIssueRequestOutboxCommandFactory {
    const val AGGREGATE_TYPE = "COUPON_ISSUE_REQUEST"

    /**
     * Request acceptance persists only the request row and this outbox command.
     * Actual issuance starts later when the worker relays the event to Kafka.
     */
    fun issueRequested(request: CouponIssueRequest): OutboxEventCommand.Publish =
        OutboxEventCommand.Publish(
            eventType = CouponOutboxEventType.COUPON_ISSUE_REQUESTED,
            aggregateType = AGGREGATE_TYPE,
            aggregateId = request.id.toString(),
            payloadJson = """{"requestId":${request.id}}""",
            dedupeKey = request.idempotencyKey,
        )
}
