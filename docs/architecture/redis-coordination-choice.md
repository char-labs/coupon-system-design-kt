# Redis Coordination Choice

## 목적

이 문서는 현재 저장소에서 Redis를 어떤 역할에 어떻게 쓰는지, 그리고 왜 같은 Redis라도
`Lua`, `Redisson`, `RedisTemplate`을 서로 다른 영역에 나눠 쓰는지 설명하는 기준 문서다.

## 한 줄 결론

- 발급 상태 reserve/release/rebuild: `Lua`
- 분산락: `Redisson Lock`
- worker 처리량 제한: `Redisson RRateLimiter`
- 일반 캐시: `RedisTemplate` 기반 `Cache`

즉, 현재 기준은 “짧고 원자적인 다중 키 상태 전이”는 Lua로, “실행 구간을 감싸는 락/리미터”는 Redisson으로 푼다.

## 현재 사용 위치

| 목적 | 구현 방식 | 대표 코드 |
| --- | --- | --- |
| 발급 reserve/release/rebuild | Spring Data Redis `DefaultRedisScript` | [CouponIssueRedisCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/coupon/CouponIssueRedisCoreRepository.kt) |
| distributed lock | Redisson `RLock` | [LockCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/lock/LockCoreRepository.kt) |
| processing limit | Redisson `RRateLimiter` | [CouponIssueProcessingLimiterCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/coupon/CouponIssueProcessingLimiterCoreRepository.kt) |
| cache get/put/evict | injected `Cache` + `CacheRepository` | [Cache.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/cache/Cache.kt) |

## 왜 reserve는 Lua인가

발급 reserve는 아래 조건을 한 번에 만족해야 한다.

- 같은 사용자가 이미 reserve 했는지 확인
- 현재 occupied count가 총 재고를 넘는지 확인
- 통과하면 count 증가와 user set 등록을 함께 반영
- TTL도 같이 갱신
- 결과를 `SUCCESS / DUPLICATE / SOLD_OUT`로 즉시 반환

이 로직은 `count key`와 `user set key` 두 개를 동시에 다룬다.
여기서 중요한 건 “짧은 시간 안에 끝나는 원자적 상태 전이”지, 장시간 락을 잡는 것이 아니다.

그래서 현재는 Lua가 가장 단순하다.

- 네트워크 왕복을 줄일 수 있다
- 다중 키 check-and-set를 한 번에 묶을 수 있다
- 반환값을 숫자 코드로 바로 매핑할 수 있다
- 비즈니스 의미가 `reserve()` 하나에 선명하게 모인다

관련 코드:

- reserve script: [CouponIssueRedisCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/coupon/CouponIssueRedisCoreRepository.kt#L90)
- release script: [CouponIssueRedisCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/coupon/CouponIssueRedisCoreRepository.kt#L119)
- rebuild script: [CouponIssueRedisCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/coupon/CouponIssueRedisCoreRepository.kt#L157)

## 왜 락은 Lua가 아니라 Redisson인가

분산락은 reserve와 성격이 다르다.
여기서 필요한 것은 “조건을 계산한 뒤 바로 값을 쓰는 원자 연산”이 아니라, 서비스 메서드 실행 구간 전체를 보호하는 것이다.

현재 락이 필요한 구간은 예를 들어 아래와 같다.

- 재고 차감 + `coupon_issue` 저장
- 취소 시 상태 전이 + 재고 복원
- 일부 상태 전이의 `REQUIRES_NEW` 실행

이런 경우에는 아래 기능이 중요하다.

- lock acquire / release 수명주기
- try/finally unlock
- timeout 처리
- AOP 기반 메서드 경계 적용
- `REQUIRES_NEW`와의 조합

이걸 전부 raw Lua로 직접 관리하면 오히려 복잡해진다.
그래서 현재는 Redisson lock을 infrastructure에 두고, 비즈니스 코드는 `@WithDistributedLock`만 사용한다.

관련 코드:

- annotation: [WithDistributedLock.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/lock/WithDistributedLock.kt)
- aspect: [DistributedLockAspect.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/lock/DistributedLockAspect.kt)
- low-level adapter: [LockCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/lock/LockCoreRepository.kt)

## 왜 processing limit도 Redisson인가

worker 처리량 제한은 “초당 permit 수를 공유”하는 문제다.
이건 reserve처럼 두 키를 직접 조합하는 상태 전이보다, 분산 rate limiter 추상화가 더 잘 맞는다.

현재 구현은 Redisson `RRateLimiter`를 사용한다.

- cluster-wide permit 공유
- 초당 permit 수 설정
- worker 여러 인스턴스가 있어도 같은 limiter key를 사용 가능

관련 코드:

- [CouponIssueProcessingLimiterCoreRepository.kt](../../storage/redis/src/main/kotlin/com/coupon/redis/coupon/CouponIssueProcessingLimiterCoreRepository.kt)

## 왜 캐시는 Lua를 쓰지 않는가

현재 캐시는 아래 성격이다.

- 단순 get-or-load
- put
- evict

즉, reserve처럼 다중 키 원자 연산이나 lock lifecycle이 핵심이 아니다.
그래서 현재는 `RedisTemplate` 기반 repository와 injected `Cache`로 충분하다.

관련 코드:

- [Cache.kt](../../coupon/coupon-domain/src/main/kotlin/com/coupon/shared/cache/Cache.kt)

## 현재 선택 기준

새로운 Redis 로직을 추가할 때는 아래 기준으로 판단한다.

### Lua가 맞는 경우

- 다중 키를 짧게 읽고 바로 같이 반영해야 할 때
- 결과를 enum/코드로 즉시 반환하면 될 때
- 메서드 실행 구간 전체를 보호할 필요가 없을 때

### Redisson lock이 맞는 경우

- 서비스 메서드 실행 구간 전체를 보호해야 할 때
- 락 획득과 해제를 try/finally로 관리해야 할 때
- AOP와 함께 쓰고 싶을 때
- `REQUIRES_NEW`와 묶어야 할 때

### Redisson rate limiter가 맞는 경우

- cluster-wide TPS 제어가 필요할 때
- permit 기반 처리량 제한이 목적일 때

### 일반 캐시/CRUD면 충분한 경우

- 단순 get/put/evict
- 단일 키 기반 TTL 관리
- 복잡한 원자 연산이 필요 없는 경우

## 지금 기준에서 바꾸지 않는 것

- reserve를 곧바로 Redisson lock 기반으로 바꾸지 않는다
- distributed lock을 raw Lua로 다시 구현하지 않는다
- cache에 과도한 Lua 스크립트를 도입하지 않는다

현재 구조에서는 역할별 도구 선택이 이미 꽤 명확하다.
