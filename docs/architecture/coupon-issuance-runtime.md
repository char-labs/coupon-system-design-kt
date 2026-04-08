# Coupon Issuance Runtime Contract

## 목적

이 문서는 현재 저장소의 쿠폰 발급 런타임 계약을 설명하는 단일 기준 문서다.
모듈 구조와 상위 아키텍처 요약은 [current-architecture-overview.md](./current-architecture-overview.md)를 먼저 본다.
Redis coordination 선택 기준은 [redis-coordination-choice.md](./redis-coordination-choice.md)를 본다.

현재 기준 구조는 아래 한 줄로 정리된다.

- `Redis reserve -> Kafka publish -> worker consume -> distributed lock -> DB persist`

`request table + relay + CDC` 구조는 현재 저장소에 없다.

## 현재 책임 분리

| 계층 | 현재 책임 | 대표 코드 |
| --- | --- | --- |
| API intake | 발급 요청 수락, 즉시 판정, Kafka publish | [CouponIssueIntakeFacade.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/intake/CouponIssueIntakeFacade.kt) |
| Worker execution | 메시지 소비, retry/DLQ, 최종 발급 반영 | [CouponIssueKafkaListener.kt](../../coupon/coupon-worker/src/main/kotlin/com/coupon/kafka/CouponIssueKafkaListener.kt) |
| Domain write | 재고 차감, 발급 row 저장, 사용/취소 상태 전이 | [CouponIssueService.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/CouponIssueService.kt) |
| Lock infra | `@WithDistributedLock` + AOP + `REQUIRES_NEW` | [DistributedLockAspect.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/lock/DistributedLockAspect.kt) |
| Redis state | duplicate 방지, stock reserve, processing limit | `storage:redis` |
| Outbox projection | lifecycle event를 `coupon_activity`로 후속 반영 | [OutboxPoller.kt](../../coupon/coupon-worker/src/main/kotlin/com/coupon/outbox/OutboxPoller.kt) |

## 용어

- `intake`: API 서버가 발급 요청을 받아 즉시 판정하는 구간
- `reserve`: Redis에서 duplicate와 stock slot을 먼저 선점하는 구간
- `publish`: reserve 성공 후 Kafka broker ack까지 받는 구간
- `consume`: worker가 Kafka 메시지를 받아 처리 결과를 결정하는 구간
- `persist`: worker가 재고 감소와 `t_coupon_issue` 저장을 끝내는 구간
- `projection`: 발급 이후 lifecycle event를 outbox 기반으로 처리하는 구간

## 공식 발급 흐름

1. `POST /coupon-issues`가 들어오면 API가 쿠폰 상세를 조회하고 발급 가능성을 검증한다.
2. API는 Redis state에서 duplicate와 stock slot을 reserve 한다.
3. reserve 결과가 `SUCCESS`일 때만 `CouponIssueMessage`를 Kafka에 발행한다.
4. Kafka broker ack를 받으면 API는 `202 Accepted`와 `SUCCESS`를 반환한다.
5. worker는 메시지를 consume 한 뒤 Redis rate limiter permit을 먼저 획득한다.
6. worker는 `couponId` 기준 `@WithDistributedLock` 안에서 재고를 감소시킨다.
7. worker는 `t_coupon_issue`를 저장하고 lifecycle domain event를 발행한다.
8. `issued/used/canceled` lifecycle event는 same transaction 안에서 `t_outbox_event`에 기록된다.
9. outbox worker가 `coupon_activity` projection을 비동기로 처리한다.

## `SUCCESS`의 의미

`SUCCESS`는 최종 DB 발급 완료가 아니다.
현재 계약에서 `SUCCESS`는 아래 두 조건이 모두 끝났다는 뜻이다.

- Redis reserve 성공
- Kafka broker ack 성공

최종 발급 row 생성과 재고 차감은 worker가 비동기로 수행한다.

## 현재 정합성 레이어

### 1. Redis reserve

- duplicate check
- stock slot reserve
- Redis state가 없으면 DB 기준 rebuild
- 구현은 Spring Data Redis `DefaultRedisScript` 기반 Lua script다.
- 이유는 `occupied-count`와 `reserved-users` 두 키를 짧고 원자적으로 함께 갱신해야 하기 때문이다.

관련 코드:

- [CouponIssueService.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/CouponIssueService.kt)
- [CouponIssueStateRepository.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/CouponIssueStateRepository.kt)
- [CouponIssueRedisCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/coupon/CouponIssueRedisCoreRepository.kt)

### 2. Distributed lock AOP

- `couponId` 또는 `couponIssueId`에서 lock key를 계산한다.
- business service는 저수준 `Lock`을 직접 주입받지 않는다.
- `requiresNew`는 lock infrastructure 안에서만 사용한다.
- 구현은 raw Lua가 아니라 Redisson lock + AOP다.
- 이유는 lock acquire/release lifecycle과 `REQUIRES_NEW` 경계를 메서드 실행 구간 단위로 다뤄야 하기 때문이다.

관련 코드:

- [WithDistributedLock.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/lock/WithDistributedLock.kt)
- [DistributedLockAspect.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/lock/DistributedLockAspect.kt)
- [RequiresNewTransactionExecutor.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/tx/RequiresNewTransactionExecutor.kt)

### 3. DB truth

- `t_coupon.remaining_quantity`
- `t_coupon_issue`
- unique constraint가 최종 duplicate 방어선이다.

### 4. Kafka retry / DLQ

- retryable failure는 Kafka error handler가 backoff 재시도한다.
- retry가 모두 소진되면 DLQ listener가 Redis reserve를 release 한다.

### 5. Worker processing limit

- worker는 consume 직후 Redisson `RRateLimiter` permit을 먼저 획득한다.
- 이 limiter는 reserve Lua script와 별개 역할이며, cluster-wide 처리량 제어만 담당한다.

## 실패 및 보상 규칙

| 구간 | 현재 동작 |
| --- | --- |
| reserve 실패 | publish 하지 않는다 |
| publish 실패 | Redis reserve를 release 하고 `COUPON_ISSUE_KAFKA_ERROR`를 반환한다 |
| worker non-retryable reject | Redis reserve를 release 하고 ack 한다 |
| worker retryable failure | retry exception을 던져 Kafka 재시도로 넘긴다 |
| DLQ 확정 | DLQ listener가 Redis reserve를 release 하고 ack 한다 |
| `ALREADY_ISSUED_COUPON` | DB unique constraint 기반 idempotent terminal result로 해석한다 |

## 상태 전이와 outbox 범위

- `use`와 `cancel`은 동기식 원본 상태 변경이다.
- outbox는 상태 전이 자체를 대신하지 않는다.
- outbox는 lifecycle 후속 projection durability를 제공한다.

현재 outbox worker가 처리하는 event:

- `COUPON_ISSUED`
- `COUPON_USED`
- `COUPON_CANCELED`

현재 projection target:

- `coupon_activity`

## 메시지 계약

`CouponIssueMessage`는 아래 필드를 가진다.

- `couponId`
- `userId`
- `requestId`
- `acceptedAt`

의미는 다음과 같다.

- `requestId`: API 로그와 worker 로그를 한 요청으로 연결
- `acceptedAt`: end-to-end 지연 계산 기준 시각

## 관측성 원칙

현재 저장소는 아래 세 축으로 관측한다.

- 기본 Micrometer/Actuator
- 구조화 로그
- Grafana/Loki 조회

복잡한 커스텀 telemetry 추상화는 두지 않는다.

### 기본 엔드포인트

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/metrics`
- `/actuator/prometheus`

### 구조화 로그 필드

- `event`
- `phase`
- `result`
- `requestId`
- `couponId`
- `userId`
- `acceptedAt`
- `durationMs`
- `errorType`

예시 phase:

- `intake.reserve`
- `intake.publish`
- `intake.compensation`
- `worker.limit`
- `worker.consume`
- `worker.dlq`

### Loki / Grafana

로컬 observability는 `Promtail`이 아니라 `Grafana Alloy`를 사용한다.
collector 선택 배경은 [loki-log-collector-choice.md](./loki-log-collector-choice.md)를 본다.

로그 예시:

```logql
{service_name="coupon-app"} | json | message =~ ".*event=coupon.issue.*phase=intake.publish.*result=FAILURE.*"
```

```logql
{service_name="coupon-worker"} | json | message =~ ".*event=coupon.issue.*phase=worker.consume.*result=RETRY.*"
```

```logql
{service_name="coupon-worker"} | json | message =~ ".*event=coupon.issue.*phase=worker.dlq.*"
```

```logql
{service_name=~"coupon-app|coupon-worker"} | json | traceId="f6fd23fa270283ee1880ce39e0c97293"
```

## 로컬 스택 기준

Docker Compose는 역할별로 분리되어 있다.

- `docker-compose.infrastructure.yml`
- `docker-compose.runtime.yml`
- `docker-compose.observability.yml`
- `docker-compose.load-test.yml`

상세는 [docker/README.md](../../docker/README.md)를 본다.

## 현재 구조에서 기억할 제약

- worker는 아직 명시적 `@ComponentScan` 예외 규칙을 사용한다.
- Redis state rebuild는 첫 reserve 요청 시점에 지연 수행된다.
- durable acceptance layer는 아직 없다.
- intake path에 outbox relay를 다시 넣지 않는다.

## 비목표

- request table / relay / CDC를 이미 구현된 것처럼 문서화하지 않는다.
- stage/prod 운영 표준을 코드 안에서 과도하게 강제하지 않는다.
- Loki/Grafana 배포 자체를 이 저장소 안에서 크게 확장하지 않는다.
