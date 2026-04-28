package com.coupon.kafka

import com.coupon.coupon.intake.CouponIssueFlowMetrics
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CouponIssueWorkerMicrometerMetrics(
    private val meterRegistry: MeterRegistry,
) : CouponIssueFlowMetrics {
    override fun recordWorkerLimit(duration: Duration) {
        timer("coupon.issue.worker.limit.duration").record(duration)
    }

    override fun recordWorkerConsume(
        result: String,
        duration: Duration,
    ) {
        counter("coupon.issue.worker.consume.count", "result", result).increment()
        timer("coupon.issue.worker.consume.duration", "result", result).record(duration)
    }

    override fun recordWorkerDlq(
        result: String,
        duration: Duration,
    ) {
        counter("coupon.issue.worker.dlq.count", "result", result).increment()
        timer("coupon.issue.worker.dlq.duration", "result", result).record(duration)
    }

    private fun counter(
        name: String,
        tagKey: String,
        tagValue: String,
    ): Counter =
        Counter
            .builder(name)
            .tag(tagKey, tagValue)
            .register(meterRegistry)

    private fun timer(name: String): Timer =
        Timer
            .builder(name)
            .publishPercentileHistogram()
            .register(meterRegistry)

    private fun timer(
        name: String,
        tagKey: String,
        tagValue: String,
    ): Timer =
        Timer
            .builder(name)
            .tag(tagKey, tagValue)
            .publishPercentileHistogram()
            .register(meterRegistry)
}
