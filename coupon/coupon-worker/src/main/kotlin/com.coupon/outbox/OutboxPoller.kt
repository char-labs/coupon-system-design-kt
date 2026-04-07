package com.coupon.outbox

import com.coupon.config.OutboxWorkerProperties
import com.coupon.support.logging.logger
import com.coupon.support.outbox.OutboxEvent
import com.coupon.support.outbox.OutboxEventService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(prefix = "worker.outbox", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OutboxPoller(
    private val outboxWorkerProperties: OutboxWorkerProperties,
    private val outboxEventService: OutboxEventService,
    private val outboxDispatcher: OutboxDispatcher,
    private val outboxWorkerMetrics: OutboxWorkerMetrics,
    private val clock: Clock,
) {
    private val log by logger()

    /**
     * Worker heartbeat.
     * It first recovers stale PROCESSING events, then claims fresh processable events and dispatches them one by one.
     */
    @Scheduled(
        fixedDelayString = "\${worker.outbox.fixed-delay}",
        initialDelayString = "\${worker.outbox.initial-delay}",
    )
    fun poll() {
        recoverStuckProcessingEvents()

        val outboxEvents =
            outboxEventService.findProcessable(
                limit = outboxWorkerProperties.batchSize,
                availableAt = LocalDateTime.ofInstant(clock.instant(), clock.zone),
            )

        outboxWorkerMetrics.recordPoll(outboxEvents.size)

        if (outboxEvents.isEmpty()) {
            return
        }

        log.debug { "Fetched ${outboxEvents.size} processable outbox events" }

        outboxEvents.forEach(::claimAndDispatch)
    }

    private fun recoverStuckProcessingEvents() {
        val recovered =
            outboxEventService.recoverStuckProcessing(
                updatedBefore = now().minus(outboxWorkerProperties.processingTimeout),
                availableAt = now(),
            )

        if (recovered > 0) {
            outboxWorkerMetrics.recordRecovered(recovered)
            log.warn { "Recovered $recovered stale outbox events stuck in PROCESSING" }
        }
    }

    private fun claimAndDispatch(event: OutboxEvent) {
        val claimed = outboxEventService.markProcessing(eventId = event.id)

        if (!claimed) {
            outboxWorkerMetrics.recordClaimSkipped(event.eventType)
            log.debug { "Skipped outbox event ${event.id} because another worker already claimed it" }
            return
        }

        outboxWorkerMetrics.recordClaimed(event.eventType)

        runCatching { outboxDispatcher.dispatch(event) }
            .onFailure { throwable ->
                log.error(throwable) { "Unexpected failure while dispatching outbox event ${event.id}" }
            }
    }

    private fun now(): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), clock.zone)
}
