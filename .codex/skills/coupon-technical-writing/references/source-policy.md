# Source Policy

## 목적

이 문서는 `coupon-technical-writing` 이 어떤 순서로 근거를 모으고, 작업용 근거 팩과
publish용 본문에 각각 어떤 방식으로 반영해야 하는지 정의한다.

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

## 작업용 근거 팩과 Publish 본문 분리

- `Local References` 는 내부 검증과 agent handoff 용도다
- publish용 블로그 본문에는 기본적으로 raw `Local References` 섹션을 남기지 않는다
- repo-local 사실은 아래 방식으로 본문에 흡수한다
  - 짧은 실제 코드 발췌
  - `코드 컨텍스트` 블록
  - 특정 함수나 흐름을 설명하는 inline grounding
  - 다이어그램 캡션과 표 설명
- 블로그 플랫폼이 허용하면 외부 링크 위주의 `References` 는 publish 본문에 둘 수 있다
- 로컬 파일 경로 나열과 상세 근거 목록은 내부 `근거 팩` 으로 유지한다

## Reference 사용 규칙

- 내부 `근거 팩` 에서는 `Local References` 와 `External References` 를 항상 분리한다
- factual claim 이 많은 문단은 필요하면 `[R1]`, `[R2]` 로 표시한다
- benchmark 블로그는 `Writing Benchmarks` 로만 적는다
- benchmark 블로그는 구조, 흐름, 소제목, 도식 활용을 참고하는 용도다
- `Writing Benchmarks` 는 기본적으로 생략한다
- 특정 benchmark 가 실제로 구조에 영향을 준 경우에만 포함하고, 각 항목에 `왜 들어갔는지` 를 한 짧은 구문으로 적는다
- topic 이 직접 관련 없는 benchmark 는 구조 영향이 명확하지 않으면 노이즈로 보고 제거한다
- benchmark 블로그만으로 기술 선택의 사실 근거를 만들지 않는다
- publish용 본문에서 로컬 근거를 쓸 때는 파일 목록 나열보다 맥락 설명을 우선한다
- `CouponIssueIntakeFacade`, `CouponIssueRedisCoreRepository` 같은 코드 단위는 본문에서 언급할 수 있지만, 그것만으로 `Local References` 섹션을 대체했다고 보지 않는다
- reviewer 는 `근거 팩` 과 `초안` 둘 다 보고, 본문이 근거를 자연스럽게 흡수했는지 확인한다

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
- 구조 영향 설명 없이 benchmark 링크를 관성적으로 남기지 않는다
- reference 없이 일반론만으로 기술 선택을 정당화하지 않는다
- 내부 `근거 팩` 을 생략하지 않는다
- publish용 본문 끝에 `Local References` 를 관성적으로 붙이지 않는다
