package com.coupon.outbox

import com.coupon.outbox.notification.slack.NoopSlackMessageSender
import com.coupon.outbox.notification.slack.SlackMessage
import com.coupon.outbox.notification.slack.SlackMessageSender
import com.coupon.shared.logging.logger
import com.coupon.shared.outbox.OutboxEvent
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SlackOutboxDeadEventNotifier(
    slackMessageSenderProvider: ObjectProvider<SlackMessageSender>,
) : OutboxDeadEventNotifier {
    private val log by logger()
    private val slackMessageSender: SlackMessageSender =
        slackMessageSenderProvider.getIfAvailable { NoopSlackMessageSender() }

    override fun notifyMarkedDead(
        event: OutboxEvent,
        reason: String,
        processedAt: LocalDateTime,
        attemptedRetryCount: Int,
    ) {
        if (!slackMessageSender.enabled) {
            return
        }

        slackMessageSender.sendMessage(
            SlackMessage(
                text = buildMessage(event, reason, processedAt, attemptedRetryCount),
            ),
        )

        log.info { "Sent Slack alert for DEAD outbox event ${event.id}" }
    }

    private fun buildMessage(
        event: OutboxEvent,
        reason: String,
        processedAt: LocalDateTime,
        attemptedRetryCount: Int,
    ): String =
        buildString {
            appendLine("[coupon-worker] Outbox event marked DEAD")
            appendLine("eventId: ${event.id}")
            appendLine("eventType: ${event.eventType}")
            appendLine("aggregate: ${event.aggregateType}:${event.aggregateId}")
            appendLine("dedupeKey: ${event.dedupeKey ?: "<none>"}")
            appendLine("retryCount: ${event.retryCount}")
            appendLine("deadAtAttempt: $attemptedRetryCount")
            appendLine("availableAt: ${event.availableAt}")
            appendLine("processedAt: $processedAt")
            append("reason: $reason")
        }
}
