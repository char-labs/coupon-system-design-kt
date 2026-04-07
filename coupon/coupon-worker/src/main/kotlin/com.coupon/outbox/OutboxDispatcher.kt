package com.coupon.outbox

import com.coupon.support.logging.logger
import com.coupon.support.outbox.OutboxEvent
import com.coupon.support.outbox.OutboxEventService
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Component
class OutboxDispatcher(
    private val outboxEventService: OutboxEventService,
    private val outboxEventHandlerRegistry: OutboxEventHandlerRegistry,
    private val outboxRetryPolicy: OutboxRetryPolicy,
    private val outboxWorkerMetrics: OutboxWorkerMetrics,
    private val clock: Clock,
) {
    private val log by logger()

    /**
     * Executes exactly one outbox event after it has already been claimed by the poller.
     * The handler returns a semantic result and the dispatcher converts that into SUCCEEDED, FAILED(backoff), or DEAD.
     */
    fun dispatch(event: OutboxEvent) {
        val startedAt = clock.instant()
        val result = dispatchInternal(event)
        val duration = Duration.between(startedAt, clock.instant())

        outboxWorkerMetrics.recordDuration(
            eventType = event.eventType,
            result = result.metricName,
            duration = duration,
        )
    }

    private fun dispatchInternal(event: OutboxEvent): DispatchOutcome {
        val handler = outboxEventHandlerRegistry.find(event.eventType)

        if (handler == null) {
            markDead(event, "No handler registered for outbox event type ${event.eventType}")
            return DispatchOutcome.DEAD
        }

        val processingResult =
            runCatching { handler.handle(event) }
                .getOrElse { throwable ->
                    OutboxProcessingResult.Retry(reason = throwable.toLastError())
                }

        return when (processingResult) {
            OutboxProcessingResult.Success -> markSucceeded(event)
            is OutboxProcessingResult.Retry -> reschedule(event, processingResult.reason, processingResult.retryAfter)
            is OutboxProcessingResult.Dead -> {
                markDead(event, processingResult.reason)
                DispatchOutcome.DEAD
            }
        }
    }

    private fun markSucceeded(event: OutboxEvent): DispatchOutcome {
        val marked =
            outboxEventService.markSucceeded(
                eventId = event.id,
                processedAt = now(),
            )

        if (marked) {
            outboxWorkerMetrics.recordSucceeded(event.eventType)
            log.debug { "Marked outbox event ${event.id} as SUCCEEDED" }
        } else {
            log.warn { "Failed to mark outbox event ${event.id} as SUCCEEDED because state transition was rejected" }
        }

        return DispatchOutcome.SUCCEEDED
    }

    private fun reschedule(
        event: OutboxEvent,
        reason: String,
        retryAfter: Duration? = null,
    ): DispatchOutcome {
        val nextRetryCount = event.retryCount + 1

        if (outboxRetryPolicy.shouldMarkDead(nextRetryCount)) {
            markDead(event, reason)
            return DispatchOutcome.DEAD
        }

        val rescheduled =
            outboxEventService.reschedule(
                eventId = event.id,
                availableAt =
                    retryAfter?.let { LocalDateTime.ofInstant(clock.instant().plus(it), clock.zone) }
                        ?: outboxRetryPolicy.nextAvailableAt(nextRetryCount),
                retryCount = nextRetryCount,
                lastError = reason,
            )

        if (rescheduled) {
            outboxWorkerMetrics.recordRetried(event.eventType)
            log.warn { "Rescheduled outbox event ${event.id} with retry count $nextRetryCount" }
        } else {
            log.warn { "Failed to reschedule outbox event ${event.id} because state transition was rejected" }
        }

        return DispatchOutcome.RETRIED
    }

    private fun markDead(
        event: OutboxEvent,
        reason: String,
    ) {
        val marked =
            outboxEventService.markDead(
                eventId = event.id,
                lastError = reason,
                processedAt = now(),
            )

        if (marked) {
            outboxWorkerMetrics.recordDead(event.eventType)
            log.error { "Marked outbox event ${event.id} as DEAD: $reason" }
        } else {
            log.warn { "Failed to mark outbox event ${event.id} as DEAD because state transition was rejected" }
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), clock.zone)

    private fun Throwable.toLastError(): String =
        buildString {
            append(this@toLastError::class.simpleName ?: "RuntimeException")
            append(": ")
            append(message ?: "Unknown error")
        }

    private enum class DispatchOutcome(
        val metricName: String,
    ) {
        SUCCEEDED("succeeded"),
        RETRIED("retried"),
        DEAD("dead"),
    }
}
