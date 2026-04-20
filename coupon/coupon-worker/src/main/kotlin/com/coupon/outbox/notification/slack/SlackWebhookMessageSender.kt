package com.coupon.outbox.notification.slack

import com.coupon.config.OutboxWorkerProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.http.HttpClient

@Component
@ConditionalOnProperty(prefix = "worker.outbox.dead-alert.slack", name = ["enabled"], havingValue = "true")
class SlackWebhookMessageSender(
    private val outboxWorkerProperties: OutboxWorkerProperties,
) : SlackMessageSender {
    override val enabled: Boolean = true

    private val slackProperties = outboxWorkerProperties.deadAlert.slack
    private val restClient: RestClient =
        RestClient
            .builder()
            .requestFactory(
                JdkClientHttpRequestFactory(
                    HttpClient
                        .newBuilder()
                        .connectTimeout(slackProperties.timeout)
                        .build(),
                ).apply {
                    setReadTimeout(slackProperties.timeout)
                },
            ).build()

    override fun sendMessage(message: SlackMessage) {
        try {
            restClient
                .post()
                .uri(slackProperties.webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("text" to message.text))
                .retrieve()
                .toBodilessEntity()
        } catch (exception: RestClientResponseException) {
            val body = exception.responseBodyAsString.ifBlank { "<empty>" }
            throw IllegalStateException(
                "Slack webhook request failed: status=${exception.statusCode.value()} body=$body",
                exception,
            )
        }
    }
}

@Component
@ConditionalOnMissingBean(SlackMessageSender::class)
class NoopSlackMessageSender : SlackMessageSender {
    override val enabled: Boolean = false

    override fun sendMessage(message: SlackMessage) {
    }
}
