package com.coupon.coupon.intake.messaging

import com.coupon.coupon.intake.CouponIssueFlowMetrics
import com.coupon.enums.coupon.CouponIssueResult
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CouponIssueIntakeMicrometerMetrics(
    private val meterRegistry: MeterRegistry,
) : CouponIssueFlowMetrics {
    override fun recordIntakeReserve(
        result: CouponIssueResult,
        duration: Duration,
    ) {
        counter("coupon.issue.intake.reserve.count", "result", result.name).increment()
        timer("coupon.issue.intake.reserve.duration", "result", result.name).record(duration)
    }

    override fun recordIntakePublish(
        result: String,
        duration: Duration,
    ) {
        counter("coupon.issue.intake.publish.count", "result", result).increment()
        timer("coupon.issue.intake.publish.duration", "result", result).record(duration)
    }

    override fun recordIntakeCompensation(result: String) {
        counter("coupon.issue.intake.compensation.count", "result", result).increment()
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
