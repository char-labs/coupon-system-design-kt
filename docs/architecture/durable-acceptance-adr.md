# ADR: Coupon Issue Durable Acceptance

## Status

Proposed.

## Context

현재 `POST /coupon-issues`의 `SUCCESS`는 최종 DB 저장 완료가 아니라 `Redis reserve + Kafka broker ack` 완료를 의미한다.
worker가 Kafka 메시지를 consume한 뒤 분산 락과 DB 저장을 수행하므로, 최종 발급 완료는 `accepted -> persist` 지표로 별도 측정해야 한다.

이 계약은 hot path를 짧게 유지하는 장점이 있지만, HTTP 응답 전후에 연결이 끊긴 경우 클라이언트가 "요청이 수락됐는지"를 안정적으로 재확인하기 어렵다.
또한 Kafka publish 실패 후 Redis release까지 실패하면 reserve 보상이 즉시 끝나지 않을 수 있다.

## Decision

이번 단계에서는 durable acceptance를 즉시 도입하지 않는다.
먼저 아래 기준선을 고정한다.

- Redis reserve 후 Kafka publish 실패 시 release 실패를 삼키지 않고 오류와 메트릭으로 노출한다.
- DLQ compensation은 Redis release 성공 전에는 ack하지 않는다.
- `SUCCESS`와 최종 DB 반영을 별도 메트릭으로 분리한다.
- k6 실행 결과는 `runId`, git SHA, scenario args를 manifest로 남긴다.

durable acceptance는 별도 이슈에서 다음 대안 중 하나로 결정한다.

## Options

### Option A. Current Direct Kafka Contract

API가 Redis reserve 후 Kafka publish ack를 기다리고 `202 Accepted`를 반환한다.

장점:

- 구현이 단순하다.
- 발급 hot path가 짧다.
- Kafka ack 전에는 성공을 반환하지 않는다.

한계:

- HTTP 전송 오류가 발생하면 클라이언트가 수락 여부를 모를 수 있다.
- publish 실패와 compensation 실패가 겹치면 별도 reconciliation이 필요하다.

### Option B. Request Table + Relay

API가 request table에 `RECEIVED/ACCEPTED`를 먼저 저장하고, relay가 Kafka로 발행한다.

장점:

- HTTP 응답을 놓쳐도 `requestId`로 상태 조회가 가능하다.
- Kafka 장애와 Redis compensation 실패를 재처리 대상으로 보존할 수 있다.

비용:

- API 계약이 `requestId` 중심으로 바뀐다.
- request table cleanup, relay 중복 발행, idempotency key 정책이 필요하다.

### Option C. Request Table + CDC

request table 변경을 CDC로 Kafka에 전달한다.

장점:

- DB commit과 이벤트 발행 사이의 내구성이 강하다.

비용:

- CDC 운영 복잡도가 크다.
- 단일 호스트/포트폴리오 환경에서는 운영 비용이 먼저 커질 수 있다.

## Follow-up Criteria

durable acceptance 도입 여부는 아래 신호가 반복될 때 다시 결정한다.

- 전송 계층 오류 후 사용자가 수락 여부를 재확인해야 하는 요구가 생긴다.
- Redis reserve compensation failure가 운영 중 반복된다.
- Kafka publish ack 대기 시간이 API p95/p99의 주된 병목이 된다.
- 프로모션 트래픽에서 request 상태 조회 UX가 필요해진다.

## Current Invariants

- `SUCCESS = Redis reserve + Kafka broker ack`.
- `persisted = worker consume + DB issue row saved`.
- DLQ는 worker consume 이후 retry exhausted를 다룬다.
- HTTP timeout, EOF, connection reset은 DLQ 대상이 아니다.
