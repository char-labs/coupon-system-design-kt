package com.coupon.outbox

import com.coupon.support.outbox.OutboxEvent
import java.time.LocalDateTime

interface OutboxDeadEventNotifier {
    fun notifyMarkedDead(
        event: OutboxEvent,
        reason: String,
        processedAt: LocalDateTime,
        attemptedRetryCount: Int,
    )
}
