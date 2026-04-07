package com.coupon.coupon.event

import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.outbox.command.OutboxEventCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import tools.jackson.databind.ObjectMapper

@Component
class CouponLifecycleOutboxListener(
    private val outboxEventService: OutboxEventService,
    private val objectMapper: ObjectMapper,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleIssued(event: CouponLifecycleDomainEvent.Issued) {
        publish(event, CouponOutboxEventType.COUPON_ISSUED)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleUsed(event: CouponLifecycleDomainEvent.Used) {
        publish(event, CouponOutboxEventType.COUPON_USED)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleCanceled(event: CouponLifecycleDomainEvent.Canceled) {
        publish(event, CouponOutboxEventType.COUPON_CANCELED)
    }

    private fun publish(
        event: CouponLifecycleDomainEvent,
        eventType: String,
    ) {
        // Lifecycle events are written before commit so DB state change and follow-up projection event stay atomic.
        outboxEventService.publish(
            OutboxEventCommand.Publish(
                eventType = eventType,
                aggregateType = "COUPON_ISSUE",
                aggregateId = event.couponIssueId.toString(),
                payloadJson = objectMapper.writeValueAsString(CouponLifecycleOutboxPayload.from(event)),
                dedupeKey = "coupon-activity:${event.couponIssueId}:$eventType",
                availableAt = event.occurredAt,
            ),
        )
    }
}
