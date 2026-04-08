package com.coupon.shared.outbox.command

import java.time.LocalDateTime

sealed interface OutboxEventCommand {
    data class Publish(
        val eventType: String,
        val aggregateType: String,
        val aggregateId: String,
        val payloadJson: String,
        val dedupeKey: String? = null,
        val availableAt: LocalDateTime = LocalDateTime.now(),
    ) : OutboxEventCommand
}
