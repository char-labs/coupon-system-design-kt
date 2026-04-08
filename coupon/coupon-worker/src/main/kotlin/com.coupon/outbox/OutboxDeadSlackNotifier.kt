package com.coupon.outbox

import com.coupon.client.slack.SlackClient
import com.coupon.client.slack.SlackMessage
import com.coupon.support.logging.logger
import com.coupon.support.outbox.OutboxEvent
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OutboxDeadSlackNotifier(
    slackClientProvider: ObjectProvider<SlackClient>,
) : OutboxDeadEventNotifier {
    private val log by logger()
    private val slackClient: SlackClient =
        slackClientProvider.getIfAvailable { DisabledSlackClient() }

    override fun notifyMarkedDead(
        event: OutboxEvent,
        reason: String,
        processedAt: LocalDateTime,
        attemptedRetryCount: Int,
    ) {
        if (!slackClient.enabled) {
            return
        }

        slackClient.sendMessage(
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

private class DisabledSlackClient : SlackClient {
    override val enabled: Boolean = false

    override fun sendMessage(message: SlackMessage) {
    }
}
