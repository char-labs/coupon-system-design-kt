# Competency Map

## 목적

이 문서는 `coupon-system-design-kt` 를 기준으로 `왜 이 선택을 했는가`를 설명하는 질문형 백엔드
글감을 정리한다.

## 질문형 글감 축

| 질문 | 글의 핵심 | 먼저 읽을 문서 | 기본 외부 묶음 | 기본 이미지 |
| --- | --- | --- | --- | --- |
| 왜 outbox 패턴을 사용하는가 | outbox 의 범위와 projection 책임 | `coupon-issuance-runtime`, `phase-2-outbox-worker-runtime` | Transactional Outbox, Polling Publisher | outbox flowchart |
| 왜 outbox 를 intake 에 쓰지 않는가 | 현재 계약과 future durability 분리 | `current-architecture-overview`, `coupon-system-expansion-todo` | Transactional Outbox, Domain Event | before/after diagram |
| 왜 Redis Lua 를 사용하는가 | 다중 키 원자 상태 전이 | `redis-coordination-choice`, `CouponIssueRedisCoreRepository` | Redis Lua scripting docs | reserve sequence diagram |
| 왜 락은 Lua 가 아니라 Redisson 인가 | 실행 구간 보호와 lifecycle | `redis-coordination-choice`, `DistributedLockAspect.kt` | Redisson lock docs | comparison table |
| 왜 Kafka 로 intake 와 execution 을 분리했는가 | acceptance 와 최종 persist 경계 | `coupon-issuance-runtime`, `coupon-kafka-runtime-guide` | Kafka intro, Kafka design, Event-Driven | intake/worker sequence diagram |
| 왜 DB unique constraint 를 마지막 duplicate 방어선으로 두는가 | 저장소 계층의 최종 정합성 | `coupon-issuance-runtime`, `CouponIssueService.kt` | Kafka design, idempotency pattern discussions | defense-in-depth diagram |
| 왜 retryable failure 와 terminal reject 를 분리했는가 | retry 가치 판단과 운영성 | `coupon-kafka-runtime-guide` | Kafka consumer semantics | failure matrix |
| 왜 phase 로그와 requestId 가 중요한가 | 운영 디버깅과 추적성 | `coupon-issuance-runtime`, `coupon-kafka-runtime-guide` | Kafka intro, event-driven references | metric/log correlation graph |

## 빠르게 쓰기 좋은 질문 큐

1. `왜 outbox 패턴을 사용하는가?`
2. `왜 outbox 를 intake durability 에 쓰지 않았는가?`
3. `Lua 스크립트는 왜 쓰는가?`
4. `왜 같은 Redis 를 Lua, Lock, Limiter 로 나눠 쓰는가?`
5. `락 대신 Kafka 를 쓴 건가? 아니면 다른 문제를 푼 건가?`
6. `왜 duplicate 방어를 DB unique constraint 까지 가져가는가?`
7. `왜 retry 와 terminal reject 를 분리해야 하는가?`

## 글을 고를 때의 기준

- 간단히 시작하려면 `왜 outbox`, `왜 Lua`, `왜 Kafka`처럼 하나의 선택을 설명하는 주제를 먼저 쓴다
- 포트폴리오용이면 `설계 이유` 와 `한계` 가 같이 드러나는 주제를 우선한다
- 실무형 독자 대상이면 `운영에서 무엇을 확인해야 하는가` 가 선명한 주제를 우선한다
- 면접 대비용이면 `왜 이 도구를 썼는가` 와 `왜 다른 대안은 지금 아니었는가` 가 선명한 주제를 우선한다
