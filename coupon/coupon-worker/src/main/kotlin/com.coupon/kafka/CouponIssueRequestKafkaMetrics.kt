package com.coupon.kafka

import com.coupon.enums.coupon.CouponIssueRequestStatus
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
class CouponIssueRequestKafkaMetrics(
    private val meterRegistry: MeterRegistry,
) {
    private val oldestAgeSeconds =
        CouponIssueRequestStatus.entries.associateWith { status ->
            AtomicLong(0).also { gauge ->
                meterRegistry.gauge(
                    "coupon.issue.request.status.oldest.age.seconds",
                    listOf(
                        io.micrometer.core.instrument.Tag
                            .of("status", status.name),
                    ),
                    gauge,
                ) { it.get().toDouble() }
            }
        }

    private val transitionCounters = ConcurrentHashMap<String, Counter>()

    fun recordRelayPublished() {
        meterRegistry.counter("coupon.issue.request.kafka.relay.success").increment()
    }

    fun recordRelayFailed() {
        meterRegistry.counter("coupon.issue.request.kafka.relay.fail").increment()
    }

    fun recordRelayDead() {
        meterRegistry.counter("coupon.issue.request.kafka.relay.dead").increment()
    }

    fun recordStatusTransition(
        from: CouponIssueRequestStatus,
        to: CouponIssueRequestStatus,
    ) {
        val key = "${from.name}->${to.name}"
        transitionCounters
            .computeIfAbsent(key) {
                Counter
                    .builder("coupon.issue.request.status.transition")
                    .tag("from", from.name)
                    .tag("to", to.name)
                    .register(meterRegistry)
            }.increment()
    }

    fun recordConsumerRetry() {
        meterRegistry.counter("coupon.issue.request.kafka.consumer.retry").increment()
    }

    fun recordErrorHandlerRetry(
        deliveryAttempt: Int,
        throwable: Throwable?,
    ) {
        meterRegistry
            .counter(
                "coupon.issue.request.kafka.consumer.error-handler.retry",
                "deliveryAttempt",
                deliveryAttempt.toString(),
                "exception",
                throwable?.let { it::class.simpleName } ?: "RuntimeException",
            ).increment()
    }

    fun recordDlq() {
        meterRegistry.counter("coupon.issue.request.kafka.dlq").increment()
    }

    fun recordOldestAge(
        status: CouponIssueRequestStatus,
        age: Duration?,
    ) {
        oldestAgeSeconds.getValue(status).set(age?.seconds ?: 0L)
    }
}
