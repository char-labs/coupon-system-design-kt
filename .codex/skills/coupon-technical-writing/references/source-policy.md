# Source Policy

## 목적

이 문서는 `coupon-technical-writing` 이 어떤 순서로 근거를 모으고, 어떤 방식으로 `References`
를 출력해야 하는지 정의한다.

## Source Hierarchy

글의 주장 근거는 아래 순서를 따른다.

1. repo 문서와 코드 anchor
2. 공식 문서와 벤더 문서
3. 패턴 원저자 또는 권위 있는 아티클
4. 작성 스타일 벤치마크용 테크 블로그

상위 근거가 충분한데 하위 근거만으로 글을 쓰지 않는다.

## 기본 Reference Bundle

블로그 초안에는 기본적으로 아래 근거 묶음을 포함한다.

- 로컬 근거 1개 이상
- 외부 factual reference 2개 이상
- 필요 시 pattern authority 1개

## Reference 사용 규칙

- `Local References` 와 `External References` 는 항상 분리한다
- factual claim 이 많은 문단은 필요하면 `[R1]`, `[R2]` 로 표시한다
- benchmark 블로그는 `Writing Benchmarks` 로만 적는다
- benchmark 블로그는 구조, 흐름, 소제목, 도식 활용을 참고하는 용도다
- benchmark 블로그만으로 기술 선택의 사실 근거를 만들지 않는다

## Topic별 Source Family

### Outbox

- Local
  - `docs/architecture/coupon-issuance-runtime.md`
  - `coupon/coupon-worker/docs/phase-2-outbox-worker-runtime.md`
- External
  - [Pattern: Transactional outbox](https://microservices.io/patterns/data/transactional-outbox)
  - [Pattern: Polling publisher](https://microservices.io/patterns/data/polling-publisher.html)
  - [Pattern: Domain event](https://microservices.io/patterns/data/domain-event.html)

### Kafka

- Local
  - `docs/architecture/coupon-issuance-runtime.md`
  - `coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md`
- External
  - [Apache Kafka Introduction](https://kafka.apache.org/41/getting-started/introduction/)
  - [Apache Kafka Design](https://kafka.apache.org/41/design/)
  - [Martin Fowler: What do you mean by “Event-Driven”?](https://martinfowler.com/articles/201701-event-driven.html)

### Redis / Lua

- Local
  - `docs/architecture/redis-coordination-choice.md`
  - `storage/redis` 의 reserve/release/rebuild 구현
- External
  - [Redis Docs: Scripting with Lua](https://redis.io/docs/latest/develop/programmability/eval-intro/)
  - [Redis Docs: Programmability](https://redis.io/docs/latest/develop/programmability/)

### Redisson lock / limiter

- Local
  - `docs/architecture/redis-coordination-choice.md`
  - lock / limiter adapter code
- External
  - [Redisson Reference Guide: Locks and synchronizers](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html)
  - [What is a Rate Limiter? | Redisson](https://redisson.pro/glossary/rate-limiter.html)

## Velog Policy

- 좋아요 30개 이상 글을 우선한다
- 공개 인덱스에서 좋아요 수가 안정적으로 보이지 않으면 `velog` 의 트렌딩/추천 노출 글을 대체 샘플로 사용한다
- 좋아요 수나 트렌딩 여부가 불확실하면 `Writing Benchmarks` 에서 `heuristic benchmark` 라고 명시한다

## 금지 규칙

- TODO 문서를 현재 구현처럼 적지 않는다
- benchmark 블로그를 공식 문서보다 앞세우지 않는다
- reference 없이 일반론만으로 기술 선택을 정당화하지 않는다
- `References` 섹션을 생략하지 않는다
