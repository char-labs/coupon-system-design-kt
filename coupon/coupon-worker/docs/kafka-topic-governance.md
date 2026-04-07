# Kafka Topic Governance

## 목적

이 문서는 쿠폰 시스템에서 Kafka topic을 어떻게 정의하고, 언제 늘리고, 어떤 기준으로 운영할지 정리한 명세 문서다.

핵심 원칙은 다음과 같다.

- topic은 비즈니스 의미 단위로 나눈다.
- source of truth는 DB request 상태다.
- topic 설계는 consumer 편의보다 정합성과 운영 추적 가능성을 우선한다.

## 현재 topic 목록

| topic | 역할 | producer | consumer | 상태 |
| --- | --- | --- | --- | --- |
| `coupon.issue.requested.v1` | accepted coupon issue request 실행 버스 | outbox relay | coupon worker consumer | 사용 중 |
| `coupon.issue.requested.v1.dlq` | request consumer retry 소진 후 격리 | Kafka error handler | DLQ listener | 사용 중 |

## topic naming 규칙

현재 저장소는 아래 규칙을 따른다.

`{domain}.{usecase}.{event}.v{version}`

예시:

- `coupon.issue.requested.v1`

DLQ는 아래 규칙을 따른다.

`{original-topic}.dlq`

예시:

- `coupon.issue.requested.v1.dlq`

## partition/key 전략

### 현재 전략

- partition count: `3`
- producer record key: `requestId.toString()`

이 선택의 의미는 다음과 같다.

- request 단위 분산은 좋다.
- 특정 couponId에 모든 메시지가 몰리지 않는다.
- 대신 couponId 기준 ordering은 Kafka가 보장하지 않는다.

현재 구조에서는 이게 맞다.

- 쿠폰별 직렬화는 Kafka가 아니라 DB 락이 담당한다.
- source of truth는 MySQL 상태 전이다.
- Kafka는 실행 버스이므로 ordering보다 분산성과 단순성이 낫다.

### key를 couponId로 바꾸지 않는 이유

`couponId`를 key로 쓰면 같은 쿠폰 요청이 같은 partition으로 몰린다.

장점:

- couponId 단위 ordering 확보

단점:

- hot coupon이 생기면 특정 partition만 과열
- consumer scale-out 효과가 줄어듦

현재는 DB lock으로 정합성을 잡고 있으므로 `requestId` key가 더 적절하다.

## replication / retention 기준

### local

- replicas: `1`
- retention: broker default 허용
- 목적: 구조 학습, 기능 확인

### stage / prod 권장

- replicas: `3`
- `min.insync.replicas`: `2`
- cleanup policy: `delete`
- retention:
  - command topic은 짧게
  - DLQ topic은 더 길게

이유:

- command topic은 장기 보관보다 재처리와 관측이 중요하다.
- DLQ는 운영자가 원인 분석할 수 있도록 더 오래 남겨야 한다.

## topic provisioning 정책

### 지금

현재는 [`CouponIssueRequestKafkaConfig.kt`](/Users/yunbeom/ybcha/coupon-system-design-kt/coupon/coupon-worker/src/main/kotlin/com.coupon/config/CouponIssueRequestKafkaConfig.kt) 의 `NewTopic` bean으로 topic을 자동 생성한다.

이 방식은 local/dev에는 좋다.

- 새로운 동료가 compose만 올려도 바로 실행된다.
- topic 누락으로 앱이 죽지 않는다.

### 앞으로

운영형 환경에서는 아래 정책으로 전환한다.

- local/dev: app-managed topic creation 허용
- staging/prod: platform-managed topic provisioning

즉, 이후 구현 계획에서는 `NewTopic` bean을 profile 또는 flag로 꺼야 한다.

## consumer group 규칙

현재 consumer group은 다음과 같다.

| group id | 역할 |
| --- | --- |
| `coupon-issue-request-group` | main request consumer |
| `coupon-issue-request-dlq-group` | DLQ final convergence |

규칙:

- main group은 실제 request execution만 담당
- DLQ group은 최종 `DEAD` 수렴만 담당
- analytics/audit용 consumer는 같은 group을 공유하지 않는다

## 확장 시 topic 추가 원칙

새 topic은 아래 조건을 만족할 때만 추가한다.

- consumer 책임이 명확히 다를 때
- retry / DLQ 전략이 다를 때
- ordering 요구사항이 다를 때
- retention 정책이 다를 때

반대로 아래 경우는 새 topic을 만들지 않는다.

- 단순히 payload field가 늘어남
- 같은 request execution을 다른 이름으로 구분하고 싶음
- consumer 코드만 조금 달라짐

## 향후 후보 topic

아직 구현하지 않았지만, 아래는 추가 후보로 볼 수 있다.

| topic | 도입 조건 |
| --- | --- |
| `coupon.lifecycle.v1` | `issued/used/canceled` fan-out consumer 분리 필요 |
| `coupon.issue.result.v1` | 외부 시스템에 발급 성공/실패 결과를 전파해야 할 때 |
| `coupon.issue.requested.v2` | payload 호환성을 유지할 수 없을 정도로 계약이 바뀔 때 |

## 운영자가 반드시 알아야 하는 것

- `coupon.issue.requested.v1` backlog가 늘었다고 바로 broker 문제는 아니다
- 먼저 `ENQUEUED` age, consumer lag, DB lock 대기, request 상태 수렴을 같이 봐야 한다
- topic partition 수를 늘리기 전에 consumer bottleneck과 DB lock 병목을 먼저 확인해야 한다

## 현재 프로젝트에서 유지할 원칙

- command topic은 적게 유지
- versioning은 이름으로 명시
- DLQ는 원본 topic suffix로 통일
- key는 request 분산 우선
- 운영형 전환 전에는 app auto topic creation 허용

## 참고 파일

- [`CouponIssueRequestKafkaProperties.kt`](../src/main/kotlin/com.coupon/config/CouponIssueRequestKafkaProperties.kt)
- [`CouponIssueRequestKafkaConfig.kt`](../src/main/kotlin/com.coupon/config/CouponIssueRequestKafkaConfig.kt)
- [`worker.yml`](../src/main/resources/worker.yml)

## 참고 링크

- [Apache Kafka: Topic naming and management basics](https://kafka.apache.org/documentation/)
- [Confluent: Kafka topic design considerations](https://developer.confluent.io/learn-kafka/architecture/get-started/)
