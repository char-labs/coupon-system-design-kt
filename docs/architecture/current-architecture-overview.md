# Current Coupon Architecture Overview

## 목적

이 문서는 2026-04 기준 현재 저장소의 모듈 구조와 런타임 책임을 한 장으로 설명하는 상위 문서다.
세부 발급 계약은 [coupon-issuance-runtime.md](./coupon-issuance-runtime.md), 운영형 Kafka 흐름은
[coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md](../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md)를 기준으로 본다.
Redis에서 왜 Lua와 Redisson을 나눠 쓰는지는 [redis-coordination-choice.md](./redis-coordination-choice.md)를 본다.

## 모듈 경계

| 모듈 | 현재 책임 | 비고 |
| --- | --- | --- |
| `coupon:coupon-api` | HTTP controller, request/response DTO, security, async config, Kafka producer wiring | 공개 API와 관리자 API의 진입점 |
| `coupon:coupon-domain` | 유스케이스 orchestration, 도메인 모델, validator, repository interface, lock/cache/tx 추상화 | 인증, 사용자, 쿠폰, 발급, outbox 공통이 함께 존재 |
| `coupon:coupon-worker` | Kafka consumer, DLQ 처리, outbox poller, dispatcher, Slack dead alert | 독립 Boot app |
| `storage:db-core` | JPA entity, Spring Data repository, domain repository 구현 | DB adapter |
| `storage:redis` | cache, distributed lock, issue state, rate limiter 구현 | Redis adapter |
| `support:logging` | JSON log, Sentry, OpenTelemetry starter | 관측성 공통 |
| `support:monitoring` | Actuator, Prometheus registry | actuator 소유 모듈 |

## 현재 런타임

| 런타임 | 역할 | 핵심 파일 |
| --- | --- | --- |
| `coupon-api` | 발급 intake, 조회, 관리자 쿠폰 관리, 인증 | [CouponServerApplication.kt](../../coupon/coupon-api/src/main/kotlin/com/coupon/CouponServerApplication.kt) |
| `coupon-worker` | Kafka consumer, retry/DLQ, outbox projection | [CouponWorkerApplication.kt](../../coupon/coupon-worker/src/main/kotlin/com/coupon/CouponWorkerApplication.kt) |
| MySQL | `coupon`, `coupon_issue`, `coupon_activity`, `outbox` 기준 데이터 | `storage:db-core` |
| Redis | duplicate 방지, stock reserve, processing limit, cache | `storage:redis` |
| Kafka | accepted coupon issue command transport | `coupon.issue.v1`, `coupon.issue.v1.dlq` |

## 현재 요청 흐름

### 1. 관리자 쿠폰 관리

- `POST /coupons`, `PUT /coupons/{id}`, `POST /coupons/{id}/activate`, `DELETE /coupons/{id}`
- 모두 동기식 DB write 경로다.
- 핵심 서비스는 [CouponService.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/CouponService.kt)다.

### 2. 공개 쿠폰 발급

- `POST /coupon-issues`
- API는 [CouponIssueController.kt](../../coupon/coupon-api/src/main/kotlin/com/coupon/controller/coupon/CouponIssueController.kt)에서 시작한다.
- 도메인 orchestration은 [CouponIssueIntakeFacade.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/intake/CouponIssueIntakeFacade.kt)가 맡는다.
- 처리 순서는 아래와 같다.
  - 쿠폰 조회 및 발급 가능성 검증
  - Redis reserve
  - Kafka publish
  - worker consume
  - 분산락 기반 최종 DB 반영

### 3. 맛집 쿠폰 발급

- `POST /restaurant-coupons/issue`
- [RestaurantCouponFacade.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/restaurant/RestaurantCouponFacade.kt)가 restaurant mapping을 coupon issue flow로 어댑트한다.
- 별도 발급 엔진을 두지 않고 일반 쿠폰 발급 경로에 합류한다.

### 4. 사용 / 취소 / 후속 projection

- `POST /coupon-issues/{couponIssueId}/use`
- `POST /coupon-issues/{couponIssueId}/cancel`
- 상태 전이는 동기식 DB write로 끝낸 뒤, lifecycle domain event를 outbox에 기록한다.
- worker outbox runtime이 `coupon_activity` projection을 비동기로 반영한다.

## 현재 설계 결정

### 트랜잭션

- 기본 write 경계는 서비스 메서드의 `@Transactional`이다.
- `REQUIRES_NEW`는 [RequiresNewTransactionExecutor.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/tx/RequiresNewTransactionExecutor.kt)로 한정한다.
- repository는 단순 save 위임보다 dirty checking이나 상태 전이처럼 실제 트랜잭션 의미가 있는 경우에만 자체 `@Transactional`을 가진다.

### 락

- 비즈니스 서비스가 저수준 `Lock`을 직접 주입받지 않는다.
- 현재 표준은 [WithDistributedLock.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/lock/WithDistributedLock.kt) + [DistributedLockAspect.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/lock/DistributedLockAspect.kt)다.
- 저수준 `Lock`과 `RequiresNewTransactionExecutor`는 lock infrastructure 안에서만 사용한다.
- reserve처럼 짧은 다중 키 상태 전이는 lock이 아니라 Redis Lua script를 사용한다.

### 캐시

- 발급 전 쿠폰 상세 조회 캐시는 injected [Cache.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/cache/Cache.kt)를 사용한다.
- 현재는 Redis remote cache만 있고 local cache layer는 없다.

### Redis coordination

- issue state reserve/release/rebuild는 Spring Data Redis `DefaultRedisScript` 기반 Lua script다.
- distributed lock과 processing limiter는 Redisson을 사용한다.
- 즉, 상태 전이는 Lua, 실행 구간 보호는 Redisson으로 나눈다.

### 비동기 경계

- 공개 발급 intake는 Kafka publish 성공까지만 책임진다.
- 최종 발급 row 저장은 worker가 담당한다.
- outbox는 intake durability가 아니라 lifecycle projection durability 용도다.

## 현재 구조의 예외와 기술 부채

### 1. worker 수동 스캔 예외

- [CouponWorkerApplication.kt](../../coupon/coupon-worker/src/main/kotlin/com/coupon/CouponWorkerApplication.kt)는 아직 명시적 `@ComponentScan`을 사용한다.
- `coupon-domain` 안에 인증, 사용자, 쿠폰, shared가 함께 있어 worker가 intake/restaurant bean만 선택적으로 제외해야 하기 때문이다.
- 즉, 현재 clean architecture의 가장 큰 구조적 예외는 worker scan rule이다.

### 2. Redis 발급 상태의 지연 복구

- [CouponIssueService.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/CouponIssueService.kt)는 reserve 시점에 Redis state가 없으면 DB 기준으로 상태를 재구성한다.
- cold start와 Redis flush에는 유효하지만, 명시적인 운영 rebuild 도구가 없다는 뜻이기도 하다.

### 3. durable acceptance 부재

- 현재 `SUCCESS`는 Redis reserve + Kafka ack 성공을 의미한다.
- request table, relay, CDC, acceptance persistence는 없다.
- durability 요구가 커지면 별도 트랙으로 다뤄야 한다.

### 4. lifecycle projection 범위 제한

- 현재 outbox worker가 처리하는 projection은 `coupon_activity` 하나다.
- projection 종류가 늘어나면 handler ownership과 fan-out 정책을 다시 정리해야 한다.

## 권장 읽기 순서

1. [coupon-issuance-runtime.md](./coupon-issuance-runtime.md)
2. [CouponIssueController.kt](../../coupon/coupon-api/src/main/kotlin/com/coupon/controller/coupon/CouponIssueController.kt)
3. [CouponIssueIntakeFacade.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/intake/CouponIssueIntakeFacade.kt)
4. [CouponIssueExecutionFacade.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/execution/CouponIssueExecutionFacade.kt)
5. [CouponIssueService.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/CouponIssueService.kt)
6. [CouponIssueKafkaListener.kt](../../coupon/coupon-worker/src/main/kotlin/com/coupon/kafka/CouponIssueKafkaListener.kt)
7. [phase-2-outbox-worker-runtime.md](../../coupon/coupon-worker/docs/phase-2-outbox-worker-runtime.md)
