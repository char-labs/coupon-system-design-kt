# Kafka Topic Governance

## 목적

이 문서는 현재 쿠폰 시스템에서 Kafka topic 을 어떻게 이름 짓고, 어떤 기준으로 운영하고, 언제 확장할지 정리한다.

## 현재 topic 목록

| topic | 역할 | producer | consumer | 상태 |
| --- | --- | --- | --- | --- |
| `coupon.issue.v1` | accepted coupon issue command bus | coupon-api | coupon-worker | 사용 중 |
| `coupon.issue.v1.dlq` | retry 소진 후 격리 | Kafka error handler | coupon-worker DLQ listener | 사용 중 |

현재는 lifecycle outbox 를 위한 별도 Kafka topic 은 없다. lifecycle 후속 projection 은 worker outbox runtime 이 DB outbox 를 직접 poll 해서 처리한다.

## naming 규칙

현재 naming 규칙:

`{domain}.{usecase}.v{version}`

예:

- `coupon.issue.v1`

DLQ 규칙:

`{original-topic}.dlq`

예:

- `coupon.issue.v1.dlq`

## partition / key 전략

현재 설정:

- partition count: `3`
- producer record key: `couponId.toString()`

의도:

- 같은 couponId 는 같은 partition 으로 보낸다
- hot coupon 내부 순서를 Kafka partition 수준에서 유지한다
- 대신 특정 hot coupon 은 한 partition 에 집중될 수 있다

이 trade-off 는 현재 설계 의도와 맞다.

## consumer group

| group id | 역할 |
| --- | --- |
| `coupon-issue-group` | main consumer |
| `coupon-issue-dlq-group` | DLQ consumer |

규칙:

- main group 은 실제 발급 실행만 담당
- DLQ group 은 최종 격리와 Redis release 만 담당
- audit / analytics consumer 는 같은 group 을 공유하지 않는다

## provisioning 정책

현재는 app-managed topic creation 을 사용한다.

관련 파일:

- [`CouponIssueKafkaConfig.kt`](../src/main/kotlin/com/coupon/config/CouponIssueKafkaConfig.kt)
- [`CouponIssueKafkaProperties.kt`](../src/main/kotlin/com/coupon/config/CouponIssueKafkaProperties.kt)

장점:

- local/dev 에서 compose 만 올려도 바로 실행 가능
- topic 누락으로 초기 개발 흐름이 끊기지 않음

운영형 권장:

- local/dev: app-managed 허용
- stage/prod: platform-managed provisioning 선호

## retention / replication 기본값

### local

- replicas: `1`
- retention: broker default

### stage / prod 권장

- replicas: `3`
- `min.insync.replicas`: `2`
- cleanup policy: `delete`
- DLQ retention 은 main topic 보다 길게

## topic 추가 원칙

새 topic 은 아래일 때만 추가한다.

- ordering 요구가 완전히 다를 때
- retry / DLQ 정책이 다를 때
- retention 정책이 다를 때
- consumer 책임이 명확히 분리될 때

아래는 새 topic 을 만들지 않는다.

- payload 필드가 조금 늘어나는 경우
- 같은 책임인데 이름만 바꾸고 싶은 경우
- 현재 consumer 안에서 충분히 분기 가능한 경우

## 향후 후보

| topic | 도입 조건 |
| --- | --- |
| `coupon.lifecycle.v1` | lifecycle projection 을 Kafka fan-out 으로 분리해야 할 때 |
| `coupon.issue.result.v1` | 외부 시스템으로 발급 결과를 내보내야 할 때 |
| `coupon.issue.v2` | payload 호환성이 깨질 정도로 계약이 바뀔 때 |

## 운영 해석 포인트

- `coupon.issue.v1` lag 증가는 broker 문제일 수도 있고 DB/lock 병목일 수도 있다
- DLQ 증가는 consumer retry 로 해결되지 않는 terminal failure 신호다
- partition 을 늘리기 전에 lock, DB pool, hot coupon 집중도를 먼저 본다

최신 전체 흐름은 [coupon-kafka-runtime-guide.md](./coupon-kafka-runtime-guide.md) 를 참고한다.
