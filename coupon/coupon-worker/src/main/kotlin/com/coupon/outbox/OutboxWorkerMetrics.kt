package com.coupon.outbox

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OutboxWorkerMetrics(
    private val meterRegistry: MeterRegistry,
    handlerRegistry: OutboxEventHandlerRegistry,
) {
    private val pollCounter = meterRegistry.counter("coupon.outbox.worker.poll.count")
    private val fetchedCounter = meterRegistry.counter("coupon.outbox.worker.poll.fetched")

    init {
        meterRegistry.gauge("coupon.outbox.worker.handler.count", handlerRegistry) { it.size().toDouble() }
    }

    fun recordPoll(fetchedCount: Int) {
        pollCounter.increment()
        fetchedCounter.increment(fetchedCount.toDouble())
    }

    fun recordRecovered(recoveredCount: Int) {
        if (recoveredCount <= 0) {
            return
        }

        meterRegistry.counter("coupon.outbox.worker.event.recovered").increment(recoveredCount.toDouble())
    }

    fun recordClaimed(eventType: String) {
        counter("coupon.outbox.worker.event.claimed", eventType).increment()
    }

    fun recordClaimSkipped(eventType: String) {
        counter("coupon.outbox.worker.event.claim.skipped", eventType).increment()
    }

    fun recordSucceeded(eventType: String) {
        counter("coupon.outbox.worker.event.succeeded", eventType).increment()
    }

    fun recordRetried(eventType: String) {
        counter("coupon.outbox.worker.event.retried", eventType).increment()
    }

    fun recordDead(eventType: String) {
        counter("coupon.outbox.worker.event.dead", eventType).increment()
    }

    fun recordDuration(
        eventType: String,
        result: String,
        duration: Duration,
    ) {
        Timer
            .builder("coupon.outbox.worker.event.duration")
            .tag("eventType", eventType)
            .tag("result", result)
            .register(meterRegistry)
            .record(duration)
    }

    private fun counter(
        name: String,
        eventType: String,
    ): Counter =
        Counter
            .builder(name)
            .tag("eventType", eventType)
            .register(meterRegistry)
}
