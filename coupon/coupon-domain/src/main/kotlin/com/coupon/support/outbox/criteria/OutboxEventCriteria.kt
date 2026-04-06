package com.coupon.support.outbox.criteria

import com.coupon.support.outbox.OutboxEventStatus
import com.coupon.support.outbox.command.OutboxEventCommand
import java.time.LocalDateTime

sealed class OutboxEventCriteria {
    data class Create(
        val eventType: String,
        val aggregateType: String,
        val aggregateId: String,
        val payloadJson: String,
        val status: OutboxEventStatus = OutboxEventStatus.PENDING,
        val dedupeKey: String? = null,
        val availableAt: LocalDateTime,
        val retryCount: Int = 0,
        val lastError: String? = null,
        val processedAt: LocalDateTime? = null,
    ) {
        companion object {
            fun of(command: OutboxEventCommand.Publish) =
                Create(
                    eventType = command.eventType,
                    aggregateType = command.aggregateType,
                    aggregateId = command.aggregateId,
                    payloadJson = command.payloadJson,
                    dedupeKey = command.dedupeKey,
                    availableAt = command.availableAt,
                )
        }
    }
}
