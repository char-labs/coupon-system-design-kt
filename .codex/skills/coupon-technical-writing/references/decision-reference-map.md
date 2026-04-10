# Decision Reference Map

## 목적

이 문서는 질문형 블로그 주제별로 어떤 로컬 문서, 코드 anchor, 외부 레퍼런스를 먼저 봐야 하는지 정리한다.

## Topic Map

| 질문 | 로컬 문서 | 코드 anchor | 외부 레퍼런스 | 메모 |
| --- | --- | --- | --- | --- |
| 왜 outbox 패턴을 사용하는가 | [coupon-issuance-runtime.md](../../../../docs/architecture/coupon-issuance-runtime.md), [phase-2-outbox-worker-runtime.md](../../../../coupon/coupon-worker/docs/phase-2-outbox-worker-runtime.md) | `CouponLifecycleOutboxListener.kt`, `OutboxPoller.kt`, `OutboxDispatcher.kt` | [Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox), [Polling Publisher](https://microservices.io/patterns/data/polling-publisher.html), [Domain event](https://microservices.io/patterns/data/domain-event.html) | 이 저장소에서는 intake durability 가 아니라 projection durability 범위로 좁혀서 쓴다 |
| 왜 outbox 를 intake durability 에 쓰지 않는가 | [current-architecture-overview.md](../../../../docs/architecture/current-architecture-overview.md), [coupon-system-expansion-todo.md](../../../../docs/architecture/coupon-system-expansion-todo.md) | `CouponIssueIntakeFacade.kt`, outbox runtime files | [Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox) | 미래 durability 트랙과 현재 계약을 분리해서 설명한다 |
| 왜 Kafka 로 intake 와 execution 을 분리했는가 | [coupon-issuance-runtime.md](../../../../docs/architecture/coupon-issuance-runtime.md), [coupon-kafka-runtime-guide.md](../../../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md) | `CouponIssueKafkaMessagePublisher.kt`, `CouponIssueKafkaListener.kt` | [Apache Kafka Introduction](https://kafka.apache.org/41/getting-started/introduction/), [Apache Kafka Design](https://kafka.apache.org/41/design/), [Martin Fowler: Event-Driven](https://martinfowler.com/articles/201701-event-driven.html) | `락 대신 Kafka` 같은 표현은 planner 가 재정의한다 |
| 왜 Redis Lua 를 사용하는가 | [redis-coordination-choice.md](../../../../docs/architecture/redis-coordination-choice.md), [coupon-issuance-runtime.md](../../../../docs/architecture/coupon-issuance-runtime.md) | `CouponIssueRedisCoreRepository.kt` | [Redis Docs: Scripting with Lua](https://redis.io/docs/latest/develop/programmability/eval-intro/), [Redis Docs: Programmability](https://redis.io/docs/latest/develop/programmability/) | 짧은 다중 키 원자 상태 전이 문제로 설명한다 |
| 왜 락은 Lua 가 아니라 Redisson 인가 | [redis-coordination-choice.md](../../../../docs/architecture/redis-coordination-choice.md) | `DistributedLockAspect.kt`, `LockCoreRepository.kt` | [Redisson Locks and synchronizers](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html) | 실행 구간 보호와 acquire/release lifecycle 로 설명한다 |
| 왜 processing limit 은 RRateLimiter 인가 | [redis-coordination-choice.md](../../../../docs/architecture/redis-coordination-choice.md), [coupon-kafka-runtime-guide.md](../../../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md) | `CouponIssueProcessingLimiter.kt`, `CouponIssueProcessingLimiterCoreRepository.kt` | [What is a Rate Limiter? | Redisson](https://redisson.pro/glossary/rate-limiter.html) | reserve Lua 와 worker limiter 의 책임을 섞지 않는다 |
| 왜 DB unique constraint 까지 duplicate 방어를 가져가는가 | [coupon-issuance-runtime.md](../../../../docs/architecture/coupon-issuance-runtime.md) | `CouponIssueService.kt`, DB entity / schema 관련 코드 | [Apache Kafka Introduction](https://kafka.apache.org/41/getting-started/introduction/), [Apache Kafka Design](https://kafka.apache.org/41/design/) | 최종 truth 는 DB 라는 점을 강조한다 |
| 왜 requestId 와 phase 로그가 필요한가 | [coupon-issuance-runtime.md](../../../../docs/architecture/coupon-issuance-runtime.md), [coupon-kafka-runtime-guide.md](../../../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md) | logging 관련 code slice | [Apache Kafka Introduction](https://kafka.apache.org/41/getting-started/introduction/), [microservices.io patterns](https://microservices.io/patterns/) | 운영 추적성과 장애 분석 관점으로 쓴다 |

## Benchmark Links

구조 참고가 필요하면 아래 글을 우선 본다.

- [올영매장은 MSA 환경에서 흩어진 도메인 데이터를 어떻게 연동했을까?](https://oliveyoung.tech/2026-03-18/oy-store-data-interconnection-strategy/)
- [QA가 서버를 죽여본 이유 – Host Level 카오스 엔지니어링 테스트](https://oliveyoung.tech/2026-03-30/chaos-host-level/)
- [모노레포 이렇게 좋은데 왜 안써요?](https://medium.com/musinsa-tech/journey-of-a-frontend-monorepo-8f5480b80661)
- [우리는 달에 가기로 했습니다](https://medium.com/musinsa-tech/%EC%9A%B0%EB%A6%AC%EB%8A%94-%EB%8B%AC%EC%97%90-%EA%B0%80%EA%B8%B0%EB%A1%9C-%ED%96%88%EC%8A%B5%EB%8B%88%EB%8B%A4-hybrid%EC%9D%B8%ED%94%84%EB%9D%BC%EB%B6%80%ED%84%B0-%EB%84%A4%ED%8A%B8%EC%9B%8C%ED%81%AC-%EC%B5%9C%EC%A0%81%ED%99%94%EA%B9%8C%EC%A7%80-%EB%AC%B4%EC%8B%A0%EC%82%AC-ai-infra%EA%B5%AC%EC%B6%95%EA%B8%B0-3ffe4831c0a4)
