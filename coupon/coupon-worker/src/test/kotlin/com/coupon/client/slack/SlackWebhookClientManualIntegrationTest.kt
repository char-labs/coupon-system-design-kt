package com.coupon.client.slack

import com.coupon.config.OutboxWorkerProperties
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

class SlackWebhookClientManualIntegrationTest {
    @Test
    fun `live webhook sends a smoke-test message`() {
        val enabled = System.getenv(LIVE_TEST_ENABLED_ENV).toBoolean()
        val webhookUrl = System.getenv(WEBHOOK_URL_ENV).orEmpty()

        assumeTrue(enabled, "$LIVE_TEST_ENABLED_ENV must be true")
        assumeTrue(webhookUrl.isNotBlank(), "$WEBHOOK_URL_ENV must be set")

        slackWebhookClient(webhookUrl).sendMessage(
            SlackMessage(
                text = "[coupon-worker] Slack webhook live smoke test at ${OffsetDateTime.now()}",
            ),
        )
    }

    private fun slackWebhookClient(webhookUrl: String): SlackWebhookClient =
        SlackWebhookClient(
            outboxWorkerProperties =
                OutboxWorkerProperties(
                    deadAlert =
                        OutboxWorkerProperties.DeadAlert(
                            slack =
                                OutboxWorkerProperties.DeadAlert.Slack(
                                    enabled = true,
                                    webhookUrl = webhookUrl,
                                    timeout = Duration.ofSeconds(3),
                                ),
                        ),
                ),
        )

    companion object {
        private const val LIVE_TEST_ENABLED_ENV = "WORKER_OUTBOX_DEAD_SLACK_LIVE_TEST_ENABLED"
        private const val WEBHOOK_URL_ENV = "WORKER_OUTBOX_DEAD_SLACK_WEBHOOK_URL"
    }
}
