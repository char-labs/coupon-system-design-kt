package com.coupon.storage.rdb.outbox

import com.coupon.support.outbox.OutboxEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxEventEntity event
           set event.status = :processingStatus,
               event.lastError = null,
               event.updatedAt = CURRENT_TIMESTAMP
         where event.id = :eventId
           and event.status in :candidateStatuses
        """,
    )
    fun markProcessing(
        eventId: Long,
        candidateStatuses: Set<OutboxEventStatus>,
        processingStatus: OutboxEventStatus,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxEventEntity event
           set event.status = :succeededStatus,
               event.processedAt = :processedAt,
               event.lastError = null,
               event.updatedAt = CURRENT_TIMESTAMP
         where event.id = :eventId
           and event.status = :processingStatus
        """,
    )
    fun markSucceeded(
        eventId: Long,
        processingStatus: OutboxEventStatus,
        succeededStatus: OutboxEventStatus,
        processedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxEventEntity event
           set event.status = :failedStatus,
               event.availableAt = :availableAt,
               event.retryCount = :retryCount,
               event.lastError = :lastError,
               event.processedAt = null,
               event.updatedAt = CURRENT_TIMESTAMP
         where event.id = :eventId
           and event.status = :processingStatus
        """,
    )
    fun reschedule(
        eventId: Long,
        processingStatus: OutboxEventStatus,
        failedStatus: OutboxEventStatus,
        availableAt: LocalDateTime,
        retryCount: Int,
        lastError: String,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxEventEntity event
           set event.status = :deadStatus,
               event.processedAt = :processedAt,
               event.lastError = :lastError,
               event.updatedAt = CURRENT_TIMESTAMP
         where event.id = :eventId
           and event.status = :processingStatus
        """,
    )
    fun markDead(
        eventId: Long,
        processingStatus: OutboxEventStatus,
        deadStatus: OutboxEventStatus,
        processedAt: LocalDateTime,
        lastError: String,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxEventEntity event
           set event.status = :failedStatus,
               event.availableAt = :availableAt,
               event.lastError = :lastError,
               event.processedAt = null,
               event.updatedAt = CURRENT_TIMESTAMP
         where event.status = :processingStatus
           and coalesce(event.updatedAt, event.createdAt) < :updatedBefore
        """,
    )
    fun recoverStuckProcessing(
        processingStatus: OutboxEventStatus,
        failedStatus: OutboxEventStatus,
        updatedBefore: LocalDateTime,
        availableAt: LocalDateTime,
        lastError: String,
    ): Int
}
