package com.coupon.coupon.event

import com.coupon.shared.outbox.OutboxEventService
import com.coupon.shared.outbox.command.OutboxEventCommand
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
        // lifecycle 이벤트는 commit 전에 함께 기록해서
        // DB 상태 변경과 후속 projection 이벤트가 같은 트랜잭션 경계 안에 남도록 한다.
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
