# Kafka DLQ and Replay Runbook

## 목적

이 문서는 쿠폰 발급 request가 Kafka retry를 모두 소진하고 DLQ로 이동했을 때, 운영자가 어떤 순서로 확인하고 어떻게 재처리할지 정리한 런북이다.

이 문서의 목표는 두 가지다.

- `DEAD` request를 애매한 상태로 방치하지 않는다.
- 운영자가 코드를 몰라도 최소한 원인 분류와 1차 대응은 할 수 있게 한다.

## 현재 구조에서 DLQ의 의미

현재 쿠폰 시스템에서 DLQ는 `최종 자동 복구 실패`를 의미한다.

흐름:

1. request가 `PENDING`으로 저장됨
2. outbox relay가 Kafka publish
3. request가 `ENQUEUED`
4. consumer가 처리 시도
5. retryable failure는 Kafka error handler 재시도
6. 재시도 소진 시 `coupon.issue.requested.v2.dlq` 로 이동
7. DLQ listener가 request를 `DEAD`로 마킹

즉, DLQ는 “메시지를 잃지 않기 위한 저장소”가 아니라 “자동 복구 범위를 벗어난 건을 사람에게 넘기는 경계”다.

## 관련 코드

- listener: [`CouponIssueRequestKafkaListener.kt`](../src/main/kotlin/com.coupon/kafka/CouponIssueRequestKafkaListener.kt)
- metrics: [`CouponIssueRequestKafkaMetrics.kt`](../src/main/kotlin/com.coupon/kafka/CouponIssueRequestKafkaMetrics.kt)
- reconciliation: [`CouponIssueRequestReconciliationService.kt`](../../coupon-domain/src/main/kotlin/com/coupon/coupon/request/CouponIssueRequestReconciliationService.kt)

## 운영 신호

아래 신호가 보이면 DLQ 또는 replay 대응이 필요하다.

- `coupon.issue.request.kafka.dlq` 증가
- `ENQUEUED` oldest age 증가
- `PROCESSING` oldest age 증가
- request 상태 중 `DEAD` 증가
- Kafka UI에서 DLQ topic 메시지 누적

## 원인 분류

DLQ는 아래 세 부류 중 하나로 본다.

### 1. payload 문제

예시:

- message schema mismatch
- aggregateId 파싱 실패
- 필수 필드 누락

조치:

- 동일 payload로는 재처리해도 다시 실패한다
- 즉시 replay하지 않는다
- producer/outbox payload 버전 문제를 먼저 수정한다

### 2. 비즈니스 상태 불일치

예시:

- request는 살아 있는데 coupon이 이미 삭제됨
- request 상태와 실제 coupon_issue row가 어긋남
- 잘못된 상태 전이

조치:

- reconciliation 결과를 먼저 본다
- request를 바로 replay하지 말고 현재 DB truth를 확인한다

### 3. 일시적 시스템 장애가 장기화된 경우

예시:

- DB lock 장기 대기
- Redis 장애
- Kafka consumer timeout 반복

조치:

- 현재는 DEAD까지 왔으므로 단순 재시도만으로 충분하지 않았다는 뜻이다
- 장애 원인 제거 후 replay 후보로 분류할 수 있다

## 운영 확인 순서

### Step 1. request 상태 확인

확인 항목:

- request id
- 현재 status
- result code
- failure reason
- last delivery error
- delivery attempt count

핵심 질문:

- 이 request는 `FAILED`가 맞는가, `DEAD`가 맞는가
- business terminal failure를 잘못 replay하려는 건 아닌가

### Step 2. 실제 coupon issue row 확인

확인 항목:

- `t_coupon_issue_request.coupon_issue_id`
- 해당 `coupon_issue_id` row 존재 여부
- 같은 `(couponId, userId)` 조합의 중복 발급 여부

핵심 질문:

- 이미 발급이 성공했는데 상태만 꼬인 것인가
- 실제 발급이 없어서 replay 대상인가

### Step 3. coupon 수량 확인

확인 항목:

- `t_coupon.remaining_quantity`
- 동일 쿠폰의 발급 건수

핵심 질문:

- request replay가 수량 정합성을 깨뜨리지 않는가

### Step 4. DLQ payload 확인

Kafka UI에서 아래를 확인한다.

- original topic
- partition
- offset
- payload
- 예외 메시지

핵심 질문:

- payload 자체가 잘못됐는가
- consumer 환경 문제였는가

## replay 판단 기준

### replay 가능

아래 조건을 모두 만족하면 replay 후보로 본다.

- request status가 `DEAD`
- 실제 `coupon_issue` row 없음
- payload가 정상
- coupon과 user 상태가 아직 발급 가능
- 원인이 일시적 시스템 장애였고 현재는 복구됨

### replay 금지

아래 중 하나면 replay하지 않는다.

- 이미 `coupon_issue` row가 존재함
- 비즈니스 실패가 명확함
  - 품절
  - 이미 발급됨
  - 잘못된 상태 전이
- payload가 malformed
- coupon이 삭제되었거나 정책상 더 이상 발급 불가

## replay 방식 원칙

현재 저장소 기준 원칙은 다음과 같다.

- Kafka DLQ message를 그대로 다시 밀어 넣는 것보다, DB request 상태를 기준으로 재발행하는 쪽이 더 안전하다
- 즉, replay는 “메시지 재전송”보다 “request 재수렴”을 우선한다

권장 방식:

1. request row를 점검
2. replay 가능 판정
3. request를 `PENDING` 또는 `ENQUEUED`로 복구하는 운영 도구 실행
4. outbox 재발행 또는 consumer 재처리

## 앞으로 필요한 구현 항목

이 문서는 현재 수동 대응 기준이다. 이후 아래 구현이 필요하다.

- admin replay endpoint 또는 운영 CLI
- replay audit log
- replay reason 기록
- “한 request를 몇 번까지 수동 replay했는지” 저장

## 운영자용 즉시 체크리스트

- DLQ message의 requestId 확인
- request row 조회
- coupon_issue row 존재 여부 확인
- 동일 user/coupon 중복 발급 여부 확인
- failure reason이 payload 문제인지, infra 문제인지 분류
- replay 가능 여부 결정
- replay 후 `ENQUEUED -> PROCESSING -> SUCCEEDED|FAILED` 수렴 확인

## 현재 프로젝트에서의 결론

- DLQ는 자동 복구 실패의 끝이다
- replay는 반드시 DB request 상태를 기준으로 한다
- payload 문제와 business terminal failure는 replay 대상이 아니다
- replay 기능은 아직 수동 운영 단계이며, 다음 개발 단계에서 도구화한다

## 참고 링크

- [Spring for Apache Kafka: Error handling and DLT](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html)
- [Confluent: Dead letter queue concepts](https://www.confluent.io/learn/kafka-dead-letter-queue/)
