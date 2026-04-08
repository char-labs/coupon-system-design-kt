# Kafka Broker Topology Evolution

## 목적

이 문서는 현재 direct Kafka coupon issue 구조를 기준으로 broker topology 를 어떻게 확장할지 정리한다.

## 현재 상태

local compose 는 단일 Kafka broker + KRaft 구성이다.

현재 기본값:

- topic: `coupon.issue.v1`
- partitions: `3`
- replicas: `1`
- producer: `acks=all`, idempotence enabled

관련 파일:

- [`CouponIssueKafkaConfig.kt`](../src/main/kotlin/com/coupon/config/CouponIssueKafkaConfig.kt)
- [`CouponIssueKafkaProperties.kt`](../src/main/kotlin/com/coupon/config/CouponIssueKafkaProperties.kt)
- [`CouponIssueKafkaMessagePublisher.kt`](../../coupon-api/src/main/kotlin/com/coupon/coupon/messaging/CouponIssueKafkaMessagePublisher.kt)

## 지금 토폴로지가 단순한 이유

- 로컬에서 빠르게 `Redis reserve -> Kafka -> worker` 흐름을 검증해야 한다
- broker cluster 운영 자체보다 쿠폰 발급 경로를 이해하는 것이 우선이다
- 단일 broker 환경에서도 partition key, retry, DLQ, lag 관찰은 충분히 가능하다

## 현재 장애 의미

broker 가 내려가면 현재 API 는 요청을 계속 수락하지 않는다.

실제 동작:

1. Redis reserve 성공
2. API 가 Kafka broker ack 대기
3. publish 실패 또는 timeout
4. Redis reserve release
5. `COUPON_ISSUE_KAFKA_ERROR`

즉, broker down 시 request 를 조용히 적재해 두는 모델이 아니다.

## stage / prod 권장값

- broker: `3`
- topic replicas: `3`
- `min.insync.replicas`: `2`
- partitions:
  - 기본 `3`
  - hot coupon 집중도가 높으면 점진 증가 검토
- producer:
  - `acks=all`
  - idempotence enabled 유지

## 언제 topology 를 키우는가

아래 신호가 같이 나타날 때 검토한다.

- `coupon.issue.v1` lag 지속 증가
- DLQ 증가
- worker consumer 는 살아 있는데 처리량이 안 올라감
- Redis reserve 는 빠른데 최종 `t_coupon_issue` 반영이 늦음
- 특정 couponId 가 지나치게 한 partition 을 과점함

단, partition 증설 전에 먼저 확인할 것:

- DB lock contention
- MySQL connection pool 포화
- worker concurrency
- hot coupon 하나에 대한 비정상 집중

## 확장 방향

### 1. broker 복제 안정화

- RF 3
- ISR 2+
- broker rolling restart 절차 확보

### 2. consumer parallelism 조정

- `worker.kafka.coupon-issue.concurrency`
- partition 수와 같이 조절

### 3. topic 분리 검토

아래는 아직 구현되지 않았지만, 필요 시 분리 후보다.

- lifecycle projection 분리 topic
- 외부 연동용 result topic

## 운영 결론

현재 구조에서 가장 중요한 질문은 “broker topology 가 충분한가” 보다 아래에 더 가깝다.

- Kafka lag 원인이 broker 인가, DB/lock 인가
- hot coupon ordering 요구를 깨지 않고 parallelism 을 얼마나 더 올릴 수 있는가

즉, broker 확장은 항상 consumer/DB/Redis 병목 분석과 같이 본다.
