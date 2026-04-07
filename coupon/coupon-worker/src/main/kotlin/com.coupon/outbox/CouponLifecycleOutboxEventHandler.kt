package com.coupon.outbox

import com.coupon.enums.coupon.CouponActivityType
import com.coupon.support.outbox.OutboxEvent

class CouponLifecycleOutboxEventHandler(
    private val support: CouponLifecycleOutboxEventHandlerSupport,
    override val eventType: String,
    private val activityType: CouponActivityType,
) : OutboxEventHandler {
    override fun handle(event: OutboxEvent): OutboxProcessingResult = support.handle(event, activityType)
}
