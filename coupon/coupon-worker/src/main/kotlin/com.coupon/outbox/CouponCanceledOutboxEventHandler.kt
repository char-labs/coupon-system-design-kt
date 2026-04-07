package com.coupon.outbox

import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.enums.coupon.CouponActivityType
import com.coupon.support.outbox.OutboxEvent
import org.springframework.stereotype.Component

@Component
class CouponCanceledOutboxEventHandler(
    private val support: CouponLifecycleOutboxEventHandlerSupport,
) : OutboxEventHandler {
    override val eventType: String = CouponOutboxEventType.COUPON_CANCELED

    override fun handle(event: OutboxEvent): OutboxProcessingResult = support.handle(event, CouponActivityType.CANCELED)
}
