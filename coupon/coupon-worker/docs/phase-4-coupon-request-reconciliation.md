# Phase 4. Coupon Request Reconciliation

> 현재 저장소에서는 더 이상 적용되지 않는 단계다.

## 무엇이 바뀌었나

예전 구조는 `t_coupon_issue_request` 와 request status machine 을 두고,

- `PENDING`
- `ENQUEUED`
- `PROCESSING`
- `SUCCEEDED`
- `FAILED`
- `DEAD`

를 reconciliation 하던 모델이었다.

현재는 공개 발급 흐름이 아래처럼 단순화되었다.

- Redis reserve
- direct Kafka publish
- worker consume
- `t_coupon_issue` 저장

따라서 다음 개념은 현재 런타임에 존재하지 않는다.

- `t_coupon_issue_request`
- request reconciliation poller
- request requeue / isolate

## 현재 복구 경로

request reconciliation 이 사라진 대신, 현재 실패 복구는 네 군데서 처리한다.

1. API publish 실패
   - Kafka broker ack timeout 또는 publish 예외 발생
   - 같은 요청 안에서 Redis reserve 를 즉시 release
2. consumer retry
   - retryable failure 는 Kafka error handler 가 backoff 재시도
3. DLQ
   - retry 소진 시 `coupon.issue.v1.dlq`
   - DLQ listener 가 Redis reserve 를 release
4. Redis rebuild
   - cold start 또는 Redis flush 이후 `CouponIssueService.rebuildState()` 로 DB 기준 state 재구성

## 지금 참고해야 할 문서

- 전체 런타임: [coupon-kafka-runtime-guide.md](./coupon-kafka-runtime-guide.md)
- DLQ 대응: [kafka-dlq-replay-runbook.md](./kafka-dlq-replay-runbook.md)
- topic 운영: [kafka-topic-governance.md](./kafka-topic-governance.md)
