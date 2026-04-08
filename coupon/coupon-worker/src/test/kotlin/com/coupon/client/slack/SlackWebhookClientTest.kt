package com.coupon.client.slack

import com.coupon.config.OutboxWorkerProperties
import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class SlackWebhookClientTest :
    BehaviorSpec({
        given("SlackWebhookClient가 incoming webhook을 호출하면") {
            `when`("Slack이 200 응답을 반환하면") {
                val server = SlackWebhookTestServer(statusCode = 200, responseBody = "ok")
                val message = SlackMessage("outbox event marked dead")
                val client = slackWebhookClient(server.url)

                server.use {
                    client.sendMessage(message)
                }

                then("JSON payload를 POST로 전송한다") {
                    server.requestMethod shouldBe "POST"
                    server.requestContentType shouldContain "application/json"
                    jacksonObjectMapper().readValue<Map<String, String>>(server.requestBody)["text"] shouldBe message.text
                }
            }

            `when`("Slack이 실패 응답을 반환하면") {
                val server = SlackWebhookTestServer(statusCode = 500, responseBody = "boom")
                val client = slackWebhookClient(server.url)

                val exception =
                    shouldThrow<IllegalStateException> {
                        server.use {
                            client.sendMessage(SlackMessage("outbox event marked dead"))
                        }
                    }

                then("예외를 던져 상위 호출자가 실패를 감지할 수 있게 한다") {
                    exception.message shouldBe "Slack webhook request failed: status=500 body=boom"
                }
            }
        }
    })

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

private class SlackWebhookTestServer(
    private val statusCode: Int,
    private val responseBody: String,
) : AutoCloseable {
    private val methodRef = AtomicReference<String>()
    private val contentTypeRef = AtomicReference<String>()
    private val bodyRef = AtomicReference<String>()
    private val server =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/services/outbox-dead") { exchange ->
                methodRef.set(exchange.requestMethod)
                contentTypeRef.set(exchange.requestHeaders.getFirst("Content-Type").orEmpty())
                bodyRef.set(exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() })

                val bodyBytes = responseBody.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(statusCode, bodyBytes.size.toLong())
                exchange.responseBody.use { output ->
                    output.write(bodyBytes)
                }
            }
            start()
        }

    val url: String
        get() = "http://127.0.0.1:${server.address.port}/services/outbox-dead"

    val requestMethod: String
        get() = methodRef.get()

    val requestContentType: String
        get() = contentTypeRef.get()

    val requestBody: String
        get() = bodyRef.get()

    override fun close() {
        server.stop(0)
    }
}
