package com.coupon.outbox

import com.coupon.coupon.activity.CouponActivityService
import com.coupon.coupon.activity.criteria.CouponActivityCriteria
import com.coupon.coupon.event.CouponLifecycleOutboxPayload
import com.coupon.enums.coupon.CouponActivityType
import com.coupon.support.outbox.OutboxEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class CouponLifecycleOutboxEventHandlerSupport(
    private val couponActivityService: CouponActivityService,
    private val objectMapper: ObjectMapper,
) {
    fun handle(
        event: OutboxEvent,
        activityType: CouponActivityType,
    ): OutboxProcessingResult {
        val payload =
            runCatching { objectMapper.readValue(event.payloadJson, CouponLifecycleOutboxPayload::class.java) }
                .getOrElse { throwable ->
                    return OutboxProcessingResult.Dead(
                        "Malformed coupon lifecycle payload for event ${event.id}: ${throwable.message ?: "unknown error"}",
                    )
                }

        couponActivityService.recordIfAbsent(CouponActivityCriteria.Create.of(payload, activityType))
        return OutboxProcessingResult.Success
    }
}
