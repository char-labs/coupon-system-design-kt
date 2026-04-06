---
name: mq-integration
description: "Kafka 또는 RabbitMQ를 Spring Boot 쿠폰 시스템에 통합하는 스킬. Docker Compose MQ 컨테이너 추가, Spring Kafka/AMQP 설정, Outbox→MQ 브릿지, Consumer, DLQ 설정을 구현한다. 'Kafka 통합', 'RabbitMQ 도입', 'MQ 연동', '메시지 큐 구현', '비동기 쿠폰 발급' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# MQ Integration

Kafka 또는 RabbitMQ를 Spring Boot 쿠폰 시스템에 통합한다.

## 구현 절차

### Step 1: ADR 확인

`_workspace/00_adr.md`를 읽고 architect가 선택한 MQ를 확인한다.
이하 절차는 선택된 MQ에 따라 분기한다.

### Step 2: Docker Compose 추가

**Kafka (KRaft 모드, Zookeeper 없음):**
```yaml
# docker/docker-compose.mq.yml
services:
  kafka:
    image: bitnami/kafka:3.7
    environment:
      KAFKA_CFG_NODE_ID: 1
      KAFKA_CFG_PROCESS_ROLES: broker,controller
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"
    ports:
      - "9092:9092"
```

**RabbitMQ:**
```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    ports:
      - "5672:5672"
      - "15672:15672"  # 관리 UI
    environment:
      RABBITMQ_DEFAULT_USER: coupon
      RABBITMQ_DEFAULT_PASS: coupon
```

### Step 3: Spring Boot 의존성 추가

**build.gradle.kts 모듈 (messaging support 또는 coupon-domain):**

```kotlin
// Kafka
implementation("org.springframework.kafka:spring-kafka")

// RabbitMQ
implementation("org.springframework.boot:spring-boot-starter-amqp")
```

### Step 4: 설정 파일

**application.yml (Kafka):**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      group-id: coupon-issue-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

### Step 5: Outbox → MQ 브릿지

기존 `OutboxEventService`를 MQ 발행 트리거로 활용한다:

```kotlin
// OutboxEventPublisher.kt (새 파일)
@Component
class OutboxEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>, // 또는 RabbitTemplate
) {
    fun publish(outboxEvent: OutboxEvent) {
        kafkaTemplate.send(TOPIC_COUPON_ISSUE, outboxEvent.aggregateId, outboxEvent.payload)
    }

    companion object {
        const val TOPIC_COUPON_ISSUE = "coupon.issue"
    }
}
```

### Step 6: Consumer 구현

```kotlin
@Component
class CouponIssueConsumer(
    private val couponIssueService: CouponIssueService,
) {
    @KafkaListener(topics = [OutboxEventPublisher.TOPIC_COUPON_ISSUE])
    fun consume(event: CouponIssueEvent) {
        // 멱등성 보장: CouponIssue에 이미 존재하면 skip
        couponIssueService.processAsync(event)
    }
}
```

### Step 7: DLQ 설정

메시지 처리 실패 시 유실 없이 DLQ로 이동:

```kotlin
@Bean
fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<*, *> {
    val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
    factory.setCommonErrorHandler(
        DefaultErrorHandler(DeadLetterPublishingRecoverer(kafkaTemplate), BackOff(1000L, 3))
    )
    return factory
}
```

### Step 8: 멱등성 보장

Consumer가 같은 메시지를 중복 처리해도 재고가 두 번 차감되지 않도록:
- `CouponIssue` 테이블에 `outbox_event_id` Unique 제약 추가
- INSERT 전 중복 체크 또는 INSERT IGNORE 사용

## 참조

상세 설정 예시: `references/kafka-config.md`, `references/rabbitmq-config.md`
