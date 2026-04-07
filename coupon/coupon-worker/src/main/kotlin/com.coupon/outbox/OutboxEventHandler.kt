package com.coupon.outbox

import com.coupon.support.outbox.OutboxEvent

interface OutboxEventHandler {
    val eventType: String

    fun handle(event: OutboxEvent): OutboxProcessingResult
}
