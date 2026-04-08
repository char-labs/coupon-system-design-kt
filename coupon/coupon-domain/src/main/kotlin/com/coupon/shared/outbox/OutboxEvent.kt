package com.coupon.shared.outbox

import java.time.LocalDateTime

data class OutboxEvent(
    val id: Long,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val payloadJson: String,
    val status: OutboxEventStatus,
    val dedupeKey: String?,
    val availableAt: LocalDateTime,
    val retryCount: Int,
    val lastError: String?,
    val processedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)
