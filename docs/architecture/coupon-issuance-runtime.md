# Coupon Issuance Runtime Contract

## 목적

이 문서는 현재 저장소의 쿠폰 발급 경로를 설명하는 단일 기준 문서다.
기준 구조는 `Redis reserve + direct Kafka publish + worker Redis processing limit + worker persist` 이며, `request table + relay + CDC` 구조는 현재 범위에 포함하지 않는다.

## 용어

- `intake`: API 서버가 발급 요청을 받아 즉시 판정하는 구간
- `reserve`: Redis에서 중복 발급과 선착순 한도를 먼저 차단하는 구간
- `publish`: reserve 성공 후 Kafka broker ack까지 받는 구간
- `consume`: worker가 Kafka 메시지를 받아 실행 결과를 결정하는 구간
- `persist`: worker가 DB 재고 감소와 `t_coupon_issue` 저장으로 최종 상태를 반영하는 구간
- `dlq`: 재시도 소진 후 별도 토픽으로 보내는 구간
- `projection`: 발급 완료 이후 outbox 기반 후속 반영 구간

## 공식 발급 흐름

1. `POST /coupon-issues` 요청이 들어오면 API 서버가 쿠폰 유효성을 확인한다.
2. API 서버는 Redis에서 duplicate/stock slot을 reserve 한다.
3. reserve 결과가 `SUCCESS`일 때만 Kafka에 `CouponIssueMessage`를 발행한다.
4. Kafka broker ack를 받으면 API는 `202 Accepted`와 `SUCCESS`를 반환한다.
5. worker는 메시지를 consume 한 뒤 Redis 기반 processing limit permit을 먼저 획득한다.
6. worker는 락을 잡고 DB 재고를 감소시킨다.
7. worker는 `t_coupon_issue`를 저장해 최종 발급 상태를 반영한다.
8. 발급 이후 상태 전이(`issued/used/canceled`)는 outbox projection으로 후속 처리한다.

## `SUCCESS`의 의미

`SUCCESS`는 DB 반영 완료가 아니다.
현재 계약에서 `SUCCESS`는 아래 두 조건이 모두 끝났음을 의미한다.

- Redis reserve 성공
- Kafka broker ack 성공

최종 발급 row 생성은 worker가 비동기로 수행한다.

## 책임 분리

- DB: 최종 truth
- Redis: admission control, duplicate 방지, 선착순 slot 관리, worker cluster-wide processing limit
- Kafka: accepted command bus
- Worker: 최종 persist와 retry/DLQ 처리
- Outbox: intake path가 아닌 후속 projection 전용

## 실패 및 보상 규칙

- reserve 실패 시 publish 하지 않는다.
- publish 실패 시 Redis reserve를 release 하고 `COUPON_ISSUE_KAFKA_ERROR`를 반환한다.
- worker가 non-retryable business error로 거절하면 Redis reserve를 release 한다.
- worker retry가 모두 소진되어 DLQ로 이동하면 DLQ consumer가 Redis reserve를 release 한다.
- `ALREADY_ISSUED_COUPON`은 DB unique constraint가 최종 정합성을 닫는 경로로 본다.

## 메시지 계약

`CouponIssueMessage`는 아래 필드를 가진다.

- `couponId`
- `userId`
- `requestId`
- `acceptedAt`

`requestId`는 API 로그, Kafka 로그, worker 로그를 한 요청으로 연결하는 용도다.
`acceptedAt`은 worker에서 end-to-end 지연을 계산하는 기준 시각이다.

## 관측성 원칙

복잡한 커스텀 telemetry 추상화는 두지 않는다.
현재 저장소는 아래 세 축으로 관측한다.

- 기본 Micrometer/Actuator: HTTP, JVM, Kafka producer/consumer, Spring Kafka observation
- 구조화 로그: business phase/result/errorType/duration
- Grafana/Loki: 구조화 로그를 기반으로 발급 흐름을 조회

로컬 docker observability는 `Promtail` 대신 `Grafana Alloy`를 사용한다.
이 선택은 Grafana 문서 기준으로 Promtail이 2026-03-02에 EOL에 도달했기 때문이다.
docker profile에서는 Logback JSON stdout을 사용하고, Alloy가 Docker logs를 읽어 Loki로 전달한다.

## 기본 엔드포인트

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/metrics`
- `/actuator/prometheus`

## 구조화 로그 필드

쿠폰 발급 관련 로그는 아래 key를 공통으로 사용한다.

- `event`
- `phase`
- `result`
- `requestId`
- `couponId`
- `userId`
- `acceptedAt`
- `durationMs`
- `errorType`

예시 phase는 아래와 같다.

- `intake.reserve`
- `intake.publish`
- `intake.compensation`
- `worker.limit`
- `worker.consume`
- `worker.dlq`

## Loki / Grafana 조회 예시

로그가 Loki로 수집되고 있다면 아래 LogQL로 바로 필터링할 수 있다.

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
{service_name="coupon-worker"} | json | message =~ ".*event=coupon.issue.*phase=worker.limit.*"
```

`durationMs`가 포함된 `intake.publish`와 `worker.consume` 로그는 Grafana에서 지연 분포 패널의 입력으로 사용할 수 있다.

로컬 Grafana provisioning에는 `coupon-runtime/Coupon Issuance Runtime` 대시보드를 포함한다.
이 대시보드는 아래 구성을 기본으로 가진다.

- `Accepted Requests`, `Publish Failures`, `Worker Success`, `DLQ Count`
- `Immediate Result Breakdown`, `Publish Outcome`, `Worker Outcome`
- `Worker Processing Limit`, `Publish Duration p95`, `Accepted To Persist p95`
- `Coupon Issue Logs`, `Failures And Retries`

추적이 필요한 경우 `RequestId regex` 변수에 특정 request id를 넣어 한 요청의 intake/worker 흐름만 좁혀서 본다.

## 운영 해석 순서

확장이나 성능 이슈는 아래 순서로 판단한다.

1. 구조화 로그로 어떤 phase에서 실패/재시도가 늘어나는지 본다.
2. `/actuator/metrics`와 Prometheus에서 HTTP/Kafka/JVM 지표를 본다.
3. DB 슬로우 쿼리, connection pool, lock contention을 확인한다.
4. 그 다음에 worker concurrency, partition 수, broker topology를 조정한다.

## 비목표

- intake path에 outbox relay를 다시 도입하지 않는다.
- stage/prod 운영 표준을 코드에서 과도하게 강제하지 않는다.
- Loki/Grafana 자체 배포 설정을 이 저장소 안에서 크게 확장하지 않는다.
