package com.coupon.support.outbox

import com.coupon.support.outbox.command.OutboxEventCommand
import com.coupon.support.outbox.criteria.OutboxEventCriteria
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OutboxEventService(
    private val outboxEventRepository: OutboxEventRepository,
) {
    companion object {
        val PROCESSABLE_STATUSES: Set<OutboxEventStatus> = setOf(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED)
    }

    fun publish(command: OutboxEventCommand.Publish): OutboxEvent =
        Tx.writeable {
            outboxEventRepository.save(OutboxEventCriteria.Create.of(command))
        }

    fun publishAll(commands: Collection<OutboxEventCommand.Publish>): List<OutboxEvent> {
        if (commands.isEmpty()) {
            return emptyList()
        }

        return Tx.writeable {
            outboxEventRepository.saveAll(commands.map(OutboxEventCriteria.Create::of))
        }
    }

    fun findById(eventId: Long): OutboxEvent =
        Tx.readable {
            outboxEventRepository.findById(eventId)
        }

    fun findProcessable(
        limit: Int,
        availableAt: LocalDateTime = LocalDateTime.now(),
    ): List<OutboxEvent> =
        Tx.readable {
            outboxEventRepository.findProcessable(
                statuses = PROCESSABLE_STATUSES,
                availableAt = availableAt,
                limit = limit,
            )
        }

    fun markProcessing(
        eventId: Long,
        candidateStatuses: Set<OutboxEventStatus> = PROCESSABLE_STATUSES,
    ): Boolean =
        Tx.writeable {
            outboxEventRepository.markProcessing(eventId, candidateStatuses)
        }

    fun markSucceeded(
        eventId: Long,
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean =
        Tx.writeable {
            outboxEventRepository.markSucceeded(eventId, processedAt)
        }

    fun reschedule(
        eventId: Long,
        availableAt: LocalDateTime,
        retryCount: Int,
        lastError: String,
    ): Boolean =
        Tx.writeable {
            outboxEventRepository.reschedule(
                eventId = eventId,
                availableAt = availableAt,
                retryCount = retryCount,
                lastError = lastError,
            )
        }

    fun markDead(
        eventId: Long,
        lastError: String,
        processedAt: LocalDateTime = LocalDateTime.now(),
    ): Boolean =
        Tx.writeable {
            outboxEventRepository.markDead(
                eventId = eventId,
                processedAt = processedAt,
                lastError = lastError,
            )
        }
}
