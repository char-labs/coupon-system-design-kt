package com.coupon.client.slack

import com.coupon.config.OutboxWorkerProperties
import com.slack.api.Slack
import com.slack.api.webhook.Payload
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "worker.outbox.dead-alert.slack", name = ["enabled"], havingValue = "true")
class SlackWebhookClient(
    private val outboxWorkerProperties: OutboxWorkerProperties,
) : SlackClient {
    override val enabled: Boolean = true

    private val slack: Slack = Slack.getInstance()

    override fun sendMessage(message: SlackMessage) {
        val slackProperties = outboxWorkerProperties.deadAlert.slack
        val response =
            slack.send(
                slackProperties.webhookUrl,
                Payload.builder().text(message.text).build(),
            )

        if (response.code != 200) {
            val body = response.body.ifBlank { "<empty>" }
            throw IllegalStateException("Slack webhook request failed: status=${response.code} body=$body")
        }
    }
}

@Component
@ConditionalOnMissingBean(SlackClient::class)
class NoopSlackClient : SlackClient {
    override val enabled: Boolean = false

    override fun sendMessage(message: SlackMessage) {
    }
}
