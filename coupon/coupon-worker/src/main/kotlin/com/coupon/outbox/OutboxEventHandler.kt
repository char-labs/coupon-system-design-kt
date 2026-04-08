package com.coupon.outbox

import com.coupon.shared.outbox.OutboxEvent

interface OutboxEventHandler {
    val eventType: String

    fun handle(event: OutboxEvent): OutboxProcessingResult
}
