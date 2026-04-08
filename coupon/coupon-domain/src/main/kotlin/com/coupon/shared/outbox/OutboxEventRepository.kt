package com.coupon.shared.outbox

import com.coupon.shared.outbox.criteria.OutboxEventCriteria
import java.time.LocalDateTime

interface OutboxEventRepository {
    fun save(criteria: OutboxEventCriteria.Create): OutboxEvent

    fun saveAll(criteria: List<OutboxEventCriteria.Create>): List<OutboxEvent>

    fun findById(eventId: Long): OutboxEvent

    fun findProcessable(
        statuses: Set<OutboxEventStatus>,
        availableAt: LocalDateTime,
        limit: Int,
    ): List<OutboxEvent>

    fun existsByAggregate(
        aggregateType: String,
        aggregateId: String,
        statuses: Set<OutboxEventStatus>,
    ): Boolean

    fun markProcessing(
        eventId: Long,
        candidateStatuses: Set<OutboxEventStatus>,
    ): Boolean

    fun markSucceeded(
        eventId: Long,
        processedAt: LocalDateTime,
    ): Boolean

    fun reschedule(
        eventId: Long,
        availableAt: LocalDateTime,
        retryCount: Int,
        lastError: String,
    ): Boolean

    fun markDead(
        eventId: Long,
        processedAt: LocalDateTime,
        lastError: String,
    ): Boolean

    fun recoverStuckProcessing(
        updatedBefore: LocalDateTime,
        availableAt: LocalDateTime,
        lastError: String,
    ): Int
}
