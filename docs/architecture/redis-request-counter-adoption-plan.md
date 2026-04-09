# Redis Request Counter Adoption Plan

## 목적

이 문서는 현재 `Lua reserve` 경로가 hot coupon 트래픽에서 실제 병목으로 확인될 때,
올리브영 글의 `3번 발급 요청 수량 체크 용도의 별도 Redis Key 추가 관리` 아이디어를
이 저장소에 맞게 어떻게 도입할지 정리한 조건부 계획서다.

중요한 전제:

- 현재 공식 기준은 여전히 `Lua reserve + Kafka publish + worker persist` 이다.
- 이 문서는 "지금 구현된 사실"이 아니라 "병목이 실측되었을 때의 전환 계획"이다.
- `SUCCESS` 의미를 바꾸지 않는다. 여전히 `Redis reserve + Kafka ack` 성공이다.
- durable acceptance, request table, relay, CDC는 이 문서의 범위가 아니다.

## 현재 기준

현재 reserve 경로는 아래 Redis 상태를 Lua 하나로 묶어 처리한다.

- `occupied-count`
- `reserved-users`

현재 보장하는 의미:

- 같은 사용자는 한 번만 reserve 된다
- reserve slot 수가 `totalQuantity` 를 넘지 않는다
- publish 실패, worker terminal reject, DLQ 확정 시 reserve 를 release 한다
- cancel 시에는 user marker 를 유지한 채 stock slot 만 반납한다

관련 문서:

- [redis-coordination-choice.md](./redis-coordination-choice.md)
- [coupon-issuance-runtime.md](./coupon-issuance-runtime.md)

## 왜 바로 "Lua 제거"가 아닌가

이 저장소는 sold-out 방지뿐 아니라 duplicate 방지도 Redis 입구에서 같이 처리한다.
그래서 올리브영 글의 이중 카운터 전략을 그대로 옮겨서 `Lua`를 즉시 제거하면,
아래 위험이 생긴다.

- duplicate marker 와 request counter 사이의 비원자적 보상 창
- cancel 이후에도 duplicate marker 를 유지해야 하는 현재 비즈니스 규칙과의 충돌
- rebuild 시점에 어떤 키를 기준으로 상태를 복원할지 애매해지는 문제

따라서 이 저장소의 1차 전환 원칙은 다음과 같다.

- 먼저 `request-count` 로 명백한 초과 요청을 Lua 앞에서 잘라낸다
- duplicate 와 authoritative reserve 판정은 당분간 기존 Lua가 계속 담당한다
- request-count prefilter 이후에도 Lua가 병목이면, 그때만 2차 설계 검토를 연다

즉, 이 저장소에서의 3번 후보안은 "Lua 즉시 대체"가 아니라
"request-count prefilter + existing Lua authoritative reserve"를 기본 경로로 잡는다.

## 전환 시작 조건

아래 조건을 모두 만족할 때만 이 계획을 시작한다.

### 1. 관측 가능성 선행

아래 지표가 먼저 준비되어 있어야 한다.

- `intake.reserve` latency
- Redis CPU 사용률
- Redis `SLOWLOG` 또는 command latency
- `worker.limit` 대기 시간
- consumer lag
- DB write latency

현재 로그에는 `intake.reserve` duration 이 없으므로, 전환 전에 먼저 계측을 보강한다.

### 2. 병목의 주체가 Lua임이 확인됨

프로덕션과 유사한 hot coupon 부하에서 아래가 동시에 확인되어야 한다.

- `reserve` latency 가 intake 지연의 상위 원인이다
- Redis CPU 또는 script latency 가 동반 상승한다
- Kafka partition, worker concurrency, processing limit, DB pool 조정만으로는 목표 TPS를 못 맞춘다
- worker lag 또는 DB write 가 1차 병목이 아니다

### 3. 정확성 회귀 없이 감당해야 할 트래픽 요구가 있음

예상 peak traffic 이 현재 Lua reserve 용량을 넘어서는 것이 수치로 확인되어야 한다.
단순한 추정이나 불안감만으로는 전환하지 않는다.

## 목표 상태

1차 목표는 request-count key를 추가해 Lua에 들어오기 전 초과 요청을 미리 거르는 것이다.

### 새 key

- `coupon:issue:state:{couponId}:request-count`

의미:

- 현재 시점에 stock slot 을 점유 중인 요청 수
- publish 성공 후 worker 완료 전 in-flight 도 포함
- 최종 발급 후에도 cancel 전까지는 유지

이 key는 의미상 현재 `occupied-count` 와 유사하지만, 전환 초기에는 별도 key로 둔다.
이유는 shadow 비교와 feature-flag rollback 을 쉽게 하기 위해서다.

### 기존 key

- `occupied-count`
- `reserved-users`

초기 rollout 동안에는 기존 Lua reserve 가 계속 authoritative path 다.

## 단계별 계획

### Phase 0. 계측 보강

목표:

- reserve 병목 여부를 수치로 확인할 수 있게 한다

작업:

- `intake.reserve` duration 로그 추가
- `request-count` 후보 key 없이도 Redis script latency, Redis CPU, `worker.limit`, consumer lag, DB latency를 한 화면에서 보게 한다
- hot coupon 1개 기준과 multi-coupon 기준 부하 테스트 템플릿을 분리한다

종료 조건:

- Lua 병목 여부를 "감"이 아니라 지표로 말할 수 있다

### Phase 1. Shadow counter 도입

목표:

- request-count key를 아직 판정에 쓰지 않고, 현재 Lua 결과를 따라가며 shadow 로만 유지한다

규칙:

- reserve `SUCCESS` 시 `request-count` 증가
- reserve `DUPLICATE` 또는 `SOLD_OUT` 시 변화 없음
- publish 실패 시 `request-count` 감소
- worker terminal reject / DLQ 시 `request-count` 감소
- cancel 성공 시 `request-count` 감소
- rebuild 시 `request-count = totalQuantity - remainingQuantity`

검증 포인트:

- `request-count` 와 `occupied-count` 가 장시간 동일하게 움직이는지 확인
- publish failure / terminal reject / DLQ / cancel 경로에서 누수 없이 보상되는지 확인

이 단계에서는 사용자 응답이나 reserve 판정에 shadow key를 절대 사용하지 않는다.

### Phase 2. Prefilter canary

목표:

- 특정 쿠폰 또는 feature flag 로만 request-count prefilter 를 활성화한다

새 intake 순서:

1. `INCR request-count`
2. 결과가 `totalQuantity` 초과면 즉시 `DECR request-count` 후 `SOLD_OUT`
3. 초과가 아니면 기존 Lua `reserve()` 실행
4. Lua 결과가 `DUPLICATE` 또는 `SOLD_OUT` 이면 `DECR request-count`
5. Lua 결과가 `SUCCESS` 면 그대로 Kafka publish 진행

의미:

- 명백히 초과된 요청은 Lua까지 보내지 않는다
- duplicate 와 authoritative reserve 의미는 기존 Lua가 계속 보장한다

검증 포인트:

- canary coupon 에서 Lua 호출 수가 유의미하게 줄어드는지 확인
- reserve 정확도 회귀가 없는지 확인
- `request-count` 누수 알람이 없는지 확인

### Phase 3. Hot coupon rollout

목표:

- hot coupon 에만 request-count prefilter 를 확대 적용한다

롤아웃 원칙:

- 전체 쿠폰 동시 전환보다 hot coupon opt-in 이 우선이다
- 운영 중에는 Lua path 를 완전히 제거하지 않는다
- rollback 은 feature flag 로 즉시 가능해야 한다

운영 알람:

- `request-count != occupied-count` 가 일정 시간 이상 유지
- publish failure / DLQ 증가
- reserve latency 개선 없음
- Redis CPU 개선 없음

### Phase 4. Full non-Lua reserve 재검토

request-count prefilter 를 넣고도 Lua가 병목이면,
그때만 "full non-Lua reserve" 를 별도 ADR로 다시 검토한다.

그 전까지는 아래를 금지한다.

- duplicate + sold-out 판정을 Lua 없이 즉시 교체
- `reserved-users` 의미를 검증 없이 바꾸기
- rebuild 규칙을 먼저 바꾸기

이 단계에서 검토할 수 있는 후보:

- per-user marker key + request-count 기반 reserve
- Redis Functions 사용 검토
- Redis Cluster 전환이 함께 필요하다면 hash tag 기반 key 재설계

단, 이 단계는 현재 문서의 범위를 넘으므로 별도 의사결정 문서가 필요하다.

## 경로별 상태 규칙

| 경로 | Lua authoritative state | request-count state |
| --- | --- | --- |
| reserve success | 증가 | 증가 |
| reserve duplicate | 변화 없음 | 증가했다면 즉시 감소 |
| reserve sold-out | 변화 없음 | 증가했다면 즉시 감소 |
| publish failure | release | 감소 |
| worker terminal reject | release | 감소 |
| DLQ 확정 | release | 감소 |
| cancel success | `releaseStockSlot` | 감소 |
| rebuild | DB truth 로 재구성 | `totalQuantity - remainingQuantity` 로 재설정 |

## rebuild 기준

rebuild 는 지금처럼 DB truth 기준으로 수행한다.

- slot count: `totalQuantity - remainingQuantity`
- duplicate marker source: `findDistinctUserIdsByCouponId(couponId)`

즉, cancel 된 사용자도 duplicate marker 에는 남는다.
request-count 는 "현재 점유 중인 slot 수"를 따르고,
duplicate marker 는 "이미 발급 이력이 있는 사용자"를 따른다.

이 규칙은 현재 저장소의 비즈니스 의미를 그대로 유지한다.

## 관측성과 알람

최소 필요 항목:

- `intake.reserve.durationMs`
- `intake.reserve.result`
- `request-count` current value
- `occupied-count` current value
- 두 값의 차이
- Redis CPU
- Redis command latency / `SLOWLOG`
- Kafka consumer lag
- `worker.limit` 대기 시간
- DB write latency

운영 알람 예시:

- `request-count - occupied-count != 0` 상태가 임계 시간 이상 지속
- `request-count` 가 음수가 되거나 `totalQuantity` 초과 상태가 지속
- request-count prefilter 활성화 후에도 reserve latency 개선이 없음

## rollback 기준

아래 중 하나라도 만족하면 request-count prefilter 를 끈다.

- accuracy regression 이 발생
- `request-count` 누수 또는 음수 상태가 반복
- reserve latency 개선폭이 미미한데 운영 복잡도만 증가
- 장애 대응 중 원인 분석 복잡도가 과도하게 증가

rollback 방식:

- feature flag 로 request-count prefilter 비활성화
- shadow write 는 유지 가능하되 판정 경로에서는 제외
- authoritative reserve 는 계속 Lua 이므로 데이터 복구 비용 없이 즉시 복귀 가능해야 한다

## 구현 원칙

- request-count 는 domain orchestration 이 아니라 Redis adapter 에 캡슐화한다
- 처음부터 전체 경로를 바꾸지 말고 shadow -> canary -> hot coupon rollout 순서로 간다
- current Lua script 와 보상 경로를 먼저 제거하지 않는다
- key naming 은 future Redis Cluster 가능성을 고려해 hash tag 도입 여부를 같이 검토한다

## 미포함 범위

이 문서가 다루지 않는 것:

- durable acceptance
- request table / relay / CDC
- DB truth 변경
- cancel 정책 변경
- duplicate 정책 변경

## 참고 자료

- [올영세일 선착순 쿠폰, 미발급 0%를 향한 여정](https://oliveyoung.tech/2025-12-15/fcfs-coupon/)
- [Redis Docs: Scripting with Lua](https://redis.io/docs/latest/develop/programmability/eval-intro/)
- [Spring Data Redis Scripting](https://docs.spring.io/spring-data/redis/reference/redis/scripting.html)
