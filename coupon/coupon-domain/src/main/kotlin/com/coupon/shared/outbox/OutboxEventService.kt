package com.coupon.shared.outbox

import com.coupon.shared.outbox.command.OutboxEventCommand
import com.coupon.shared.outbox.criteria.OutboxEventCriteria
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class OutboxEventService(
    private val outboxEventRepository: OutboxEventRepository,
) {
    companion object {
        val PROCESSABLE_STATUSES: Set<OutboxEventStatus> = setOf(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED)
        val ACTIVE_STATUSES: Set<OutboxEventStatus> =
            setOf(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED, OutboxEventStatus.PROCESSING)
    }

    @Transactional
    fun publish(command: OutboxEventCommand.Publish): OutboxEvent = outboxEventRepository.save(OutboxEventCriteria.Create.of(command))

    @Transactional
    fun publishAll(commands: Collection<OutboxEventCommand.Publish>): List<OutboxEvent> {
        if (commands.isEmpty()) {
            return emptyList()
        }

        return outboxEventRepository.saveAll(commands.map(OutboxEventCriteria.Create::of))
    }

    fun findById(eventId: Long): OutboxEvent = outboxEventRepository.findById(eventId)

    fun findProcessable(
        limit: Int,
        availableAt: LocalDateTime = LocalDateTime.now(),
    ): List<OutboxEvent> =
        outboxEventRepository.findProcessable(
            statuses = PROCESSABLE_STATUSES,
            availableAt = availableAt,
            limit = limit,
        )

    fun existsActiveEvent(
        aggregateType: String,
        aggregateId: String,
        statuses: Set<OutboxEventStatus> = ACTIVE_STATUSES,
    ): Boolean =
        outboxEventRepository.existsByAggregate(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            statuses = statuses,
        )

    @Transactional
    fun markProcessing(
        eventId: Long,
        candidateStatuses: Set<OutboxEventStatus> = PROCESSABLE_STATUSES,
    ): Boolean = outboxEventRepository.markProcessing(eventId, candidateStatuses)

    @Transactional
    fun markSucceeded(
        eventId: Long,
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean = outboxEventRepository.markSucceeded(eventId, processedAt)

    @Transactional
    fun reschedule(
        eventId: Long,
        availableAt: LocalDateTime,
        retryCount: Int,
        lastError: String,
    ): Boolean =
        outboxEventRepository.reschedule(
            eventId = eventId,
            availableAt = availableAt,
            retryCount = retryCount,
            lastError = lastError,
        )

    @Transactional
    fun markDead(
        eventId: Long,
        lastError: String,
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean =
        outboxEventRepository.markDead(
            eventId = eventId,
            processedAt = processedAt,
            lastError = lastError,
        )

    @Transactional
    fun recoverStuckProcessing(
        updatedBefore: LocalDateTime,
        availableAt: LocalDateTime = LocalDateTime.now(),
        lastError: String = "Recovered stale PROCESSING event",
    ): Int =
        outboxEventRepository.recoverStuckProcessing(
            updatedBefore = updatedBefore,
            availableAt = availableAt,
            lastError = lastError,
        )
}
