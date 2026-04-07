package com.coupon.support.testing

import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.UUID

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class CouponWorkerKafkaIntegrationSupport {
    protected fun awaitUntilAsserted(block: () -> Unit) {
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(block)
    }

    companion object {
        private val TOPIC_SUFFIX =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(8)
        private const val REDIS_PORT = 6379

        private val kafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
        private val redisContainer: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(REDIS_PORT)

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            startContainers()

            registry.add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
            registry.add("core.storage.redis.host") { redisContainer.host }
            registry.add("core.storage.redis.port") { redisContainer.getMappedPort(REDIS_PORT) }
            registry.add("worker.kafka.coupon-issue.topic") { "coupon.issue.$TOPIC_SUFFIX" }
            registry.add("worker.kafka.coupon-issue.dlq-topic") { "coupon.issue.$TOPIC_SUFFIX.dlq" }
            registry.add("worker.kafka.coupon-issue.group-id") { "coupon-issue-group-$TOPIC_SUFFIX" }
            registry.add("worker.kafka.coupon-issue.dlq-group-id") { "coupon-issue-dlq-group-$TOPIC_SUFFIX" }
        }

        @JvmStatic
        @AfterAll
        fun stopContainers() {
            if (redisContainer.isRunning) {
                redisContainer.stop()
            }
            if (kafkaContainer.isRunning) {
                kafkaContainer.stop()
            }
        }

        private fun startContainers() {
            if (!redisContainer.isRunning) {
                redisContainer.start()
            }
            if (!kafkaContainer.isRunning) {
                kafkaContainer.start()
            }
        }
    }
}
