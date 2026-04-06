package com.coupon.storage.rdb.outbox

import com.coupon.storage.rdb.support.findByIdOrElseThrow
import com.coupon.support.outbox.OutboxEvent
import com.coupon.support.outbox.OutboxEventRepository
import com.coupon.support.outbox.OutboxEventStatus
import com.coupon.support.outbox.criteria.OutboxEventCriteria
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class OutboxEventCoreRepository(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {
    override fun save(criteria: OutboxEventCriteria.Create): OutboxEvent =
        outboxEventJpaRepository
            .save(OutboxEventEntity(criteria))
            .toOutboxEvent()

    override fun saveAll(criteria: List<OutboxEventCriteria.Create>): List<OutboxEvent> {
        if (criteria.isEmpty()) {
            return emptyList()
        }

        return outboxEventJpaRepository
            .saveAll(criteria.map(::OutboxEventEntity))
            .map(OutboxEventEntity::toOutboxEvent)
    }

    override fun findById(eventId: Long): OutboxEvent = outboxEventJpaRepository.findByIdOrElseThrow(eventId).toOutboxEvent()

    override fun findProcessable(
        statuses: Set<OutboxEventStatus>,
        availableAt: LocalDateTime,
        limit: Int,
    ): List<OutboxEvent> {
        val pageable = PageRequest.of(0, limit)
        return outboxEventJpaRepository.findProcessable(statuses, availableAt, pageable).map(OutboxEventEntity::toOutboxEvent)
    }

    override fun markProcessing(
        eventId: Long,
        candidateStatuses: Set<OutboxEventStatus>,
    ): Boolean =
        outboxEventJpaRepository.markProcessing(
            eventId = eventId,
            candidateStatuses = candidateStatuses,
            processingStatus = OutboxEventStatus.PROCESSING,
        ) > 0

    override fun markSucceeded(
        eventId: Long,
        processedAt: LocalDateTime,
    ): Boolean =
        outboxEventJpaRepository.markSucceeded(
            eventId = eventId,
            processingStatus = OutboxEventStatus.PROCESSING,
            succeededStatus = OutboxEventStatus.SUCCEEDED,
            processedAt = processedAt,
        ) > 0

    override fun reschedule(
        eventId: Long,
        availableAt: LocalDateTime,
        retryCount: Int,
        lastError: String,
    ): Boolean =
        outboxEventJpaRepository.reschedule(
            eventId = eventId,
            processingStatus = OutboxEventStatus.PROCESSING,
            failedStatus = OutboxEventStatus.FAILED,
            availableAt = availableAt,
            retryCount = retryCount,
            lastError = lastError,
        ) > 0

    override fun markDead(
        eventId: Long,
        processedAt: LocalDateTime,
        lastError: String,
    ): Boolean =
        outboxEventJpaRepository.markDead(
            eventId = eventId,
            processingStatus = OutboxEventStatus.PROCESSING,
            deadStatus = OutboxEventStatus.DEAD,
            processedAt = processedAt,
            lastError = lastError,
        ) > 0
}
