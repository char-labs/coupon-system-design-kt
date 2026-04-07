# Kafka DLQ Replay Runbook

## 목적

이 문서는 현재 direct Kafka issue flow 에서 메시지가 DLQ 로 이동했을 때 운영자가 어떤 순서로 확인하고 어떻게 재시도할지 정리한 런북이다.

현재 기준 topic:

- main: `coupon.issue.v1`
- DLQ: `coupon.issue.v1.dlq`

## 현재 DLQ 의미

DLQ 로 이동했다는 뜻은 아래와 같다.

- Redis reserve 는 성공했다
- Kafka publish 도 성공했다
- worker consumer 가 여러 번 재시도했지만 끝내 처리하지 못했다
- DLQ listener 가 Redis reserve 를 release 했다

즉, DLQ 이후에는 같은 `couponId/userId` 조합이 Redis 에 묶여 있지 않아야 한다.

## 가장 먼저 볼 것

1. `coupon-worker` 로그에서 DLQ 원인 확인
2. Kafka UI 에서 `coupon.issue.v1.dlq` 적재 메시지 확인
3. 같은 `couponId/userId` 의 `t_coupon_issue` row 존재 여부 확인
4. Redis state 가 이미 release 되었는지 확인

## 확인 순서

### Step 1. 이미 발급이 끝난 건 아닌지 확인

DLQ 전에 worker 가 마지막 commit 직전까지 갔을 수는 없지만, 운영상 가장 먼저 확인할 값은 DB truth 다.

확인 대상:

- `t_coupon_issue` 에 같은 `coupon_id`, `user_id` row 가 있는가
- `t_coupon.remaining_quantity` 가 이미 감소했는가

판단:

- 발급 row 가 있으면 manual replay 는 하지 않는다
- 발급 row 가 없으면 다음 단계로 진행한다

### Step 2. Redis reserve 가 풀렸는지 확인

현재 DLQ listener 는 아래 보상을 수행한다.

- reserved user marker release
- occupied count release

그래서 동일 사용자가 다시 발급을 시도할 수 있어야 한다.

관련 파일:

- [`CouponIssueKafkaListener.kt`](../src/main/kotlin/com.coupon/kafka/CouponIssueKafkaListener.kt)
- [`CouponIssueService.kt`](../../coupon-domain/src/main/kotlin/com/coupon/coupon/CouponIssueService.kt)

### Step 3. 재시도 방식을 결정한다

권장 순서는 아래와 같다.

1. 가능하면 같은 비즈니스 요청을 다시 호출한다
   - `POST /coupon-issues`
   - 이유: 현재 구조에는 request table 이 없어서, 가장 안전한 재진입점은 공개 API 이다
2. 운영상 꼭 필요할 때만 Kafka message replay 를 검토한다
   - 이 경우에도 먼저 DB truth 와 Redis state 가 깨끗한지 확인한다

## 권장 대응 시나리오

### A. 일시 장애였다

예:

- DB connection 일시 오류
- lock contention spike
- worker restart 중간 구간

대응:

1. worker / DB / Redis 상태 정상화
2. 동일 사용자의 발급 재시도 유도 또는 운영 도구로 API 재호출
3. `t_coupon_issue` 생성 여부 확인

### B. payload 또는 코드 버그였다

예:

- 역직렬화 문제
- 잘못된 validation
- 특정 coupon 상태 버그

대응:

1. 원인 코드 수정
2. 발급 성공 row 가 없는 것만 선별
3. 필요한 건만 재호출

## 하지 말아야 할 것

- 존재하지 않는 request row 를 복구하려고 하지 않는다
- 과거 `PENDING/ENQUEUED` 상태 머신 문서를 기준으로 수동 DB 보정을 하지 않는다
- `t_coupon_issue` row 존재 여부를 보지 않고 DLQ 메시지를 무조건 replay 하지 않는다

## 운영 체크포인트

- DLQ 증가량
- main topic lag
- worker retry 로그
- `t_coupon_issue` 생성 실패 패턴
- Redis release 누락 여부

최신 전체 흐름은 [coupon-kafka-runtime-guide.md](./coupon-kafka-runtime-guide.md) 를 본다.
