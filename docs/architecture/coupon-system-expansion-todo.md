# Coupon System Expansion TODO

## 현재 기준

- 발급 계약의 공식 기준은 `Redis reserve + Kafka publish + worker persist` 이다.
- outbox는 intake durability가 아니라 lifecycle projection durability 용도다.
- 락 표준은 `@WithDistributedLock`, 트랜잭션 표준은 서비스 경계 `@Transactional`이다.
- Loki collector 기본값은 Promtail이 아니라 Alloy다.
- Docker Compose는 `infrastructure / runtime / observability / load-test`로 분리되어 있다.

## 아키텍처 우선 TODO

### 1. worker 수동 component scan 축소

현재 [CouponWorkerApplication.kt](../../coupon/coupon-worker/src/main/kotlin/com/coupon/CouponWorkerApplication.kt)는 명시적 `@ComponentScan`과 exclude filter를 사용한다.
원인은 `coupon-domain` 안에 auth, user, coupon, shared가 함께 있어 worker가 intake/restaurant bean만 선택적으로 제외해야 하기 때문이다.

다음 리팩토링 목표:

- worker가 exclude filter 없이 필요한 slice만 자연스럽게 읽도록 경계 재정리
- 최소 후보:
  - intake 전용 slice 분리
  - worker 전용 execution/outbox 의존성 경계 명시화

### 2. Redis issue state 운영 도구 보강

현재 Redis issue state는 첫 reserve 요청 시점에 lazy rebuild 된다.
운영상 필요한 다음 기능은 아직 없다.

- 명시적 rebuild admin command
- 특정 couponId state 진단 도구
- rebuild 소요 시간/건수 관측 지표

## 신뢰성 TODO

### 1. durable acceptance 트랙 분리 검토

현재 `SUCCESS`는 Redis reserve + Kafka ack 성공이다.
아래 요구가 생기면 별도 트랙으로 다뤄야 한다.

- acceptance 자체를 영속적으로 남겨야 하는 경우
- publish 직후 장애에서 재생 가능성을 더 강하게 요구하는 경우

후보 방향:

- request table
- relay / outbox
- CDC

단, 현재 저장소에는 아직 구현되지 않았다.

### 2. outbox fan-out 확장 기준 정의

현재 outbox projection 대상은 사실상 `coupon_activity` 하나다.
projection 종류가 늘어날 때 아래 기준이 필요하다.

- handler ownership 분리
- event payload versioning
- DEAD alert 우선순위 분리

### 3. hot coupon 병목 실측 기준 축적

현재 조정 가능한 축:

- Kafka partition 수
- worker concurrency
- Redis processing limit
- DB pool / lock contention

남은 TODO:

- 어떤 지표 조합에서 partition을 늘릴지 기준 문서화
- `worker.limit`, consumer lag, DB write latency를 함께 보는 실측 템플릿 정리

## 관측성 TODO

- provisioned `Coupon Issuance Runtime` 대시보드와 실제 로그 필드 계약이 계속 일치하는지 유지
- 운영 환경 Loki label 기준(`service_name`, `env`, `traceId`) 문서화
- load test 결과와 발급 로그를 함께 해석하는 운영 체크리스트 보강
- 로그만으로 부족해질 때에만 business counter/timer를 Prometheus에 추가

## 플랫폼 TODO

- stage/prod topic creation을 platform-managed 방식으로 분리
- local/dev는 현재처럼 app-managed topic creation 유지
- 운영 요구가 생기면 아래 항목을 별도 트랙으로 검토
  - Redis Cluster sharding
  - broker multi-node HA
  - autoscaling
