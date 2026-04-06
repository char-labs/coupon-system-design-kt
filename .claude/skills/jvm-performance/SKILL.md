---
name: jvm-performance
description: "Spring Boot 쿠폰 시스템의 JVM, 커넥션 풀, 비동기 스레드, Redis 사용 패턴을 10k RPS 목표에 맞게 최적화하는 스킬. HikariCP 풀 크기 계산, Tomcat 스레드 튜닝, GC 설정, async 스레드 풀 조정을 수행한다. 'JVM 튜닝', '성능 최적화', '커넥션 풀 조정', 'HikariCP', '스레드 풀 설정', '처리량 개선' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# JVM Performance Optimization

단일 Spring Boot 인스턴스에서 10k RPS를 처리할 수 있도록 JVM과 Spring 설정을 최적화한다.

## 최적화 절차

### Step 1: 현재 설정 파악

다음 파일들을 읽어 현재 설정을 확인한다:
- `coupon/coupon-api/src/main/resources/application.yml` (또는 application.properties)
- `coupon/coupon-api/src/main/kotlin/com.coupon/config/AsyncConfig.kt`
- `docker/docker-compose.yml` (JVM 플래그, 메모리 설정)

### Step 2: HikariCP 커넥션 풀 최적화

리틀의 법칙 기반으로 계산한다:

```
필요 커넥션 수 = 목표RPS × 평균DB응답시간(초)
               = 10,000 × 0.005 = 50개
```

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50        # 계산값, 실측 후 조정
      minimum-idle: 10
      connection-timeout: 3000     # 3초 초과 시 빠른 실패
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 5000
```

### Step 3: Tomcat 스레드 풀 최적화

```yaml
server:
  tomcat:
    threads:
      max: 200          # 10k RPS + 10ms 응답 = 100 concurrent, 여유 2배
      min-spare: 20
    accept-count: 100
    connection-timeout: 5000
```

### Step 4: AsyncConfig 스레드 풀 조정

`AsyncConfig.kt`를 읽고 현재 설정을 확인한 뒤 조정한다:

```kotlin
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 20
        maxPoolSize = 100
        queueCapacity = 1000
        setThreadNamePrefix("async-")
        initialize()
    }
}
```

### Step 5: JVM 플래그 (Docker)

`docker-compose.yml`의 Java 옵션을 추가한다:

```yaml
environment:
  JAVA_OPTS: >-
    -XX:+UseZGC
    -Xms512m
    -Xmx2g
    -XX:MaxGCPauseMillis=10
    -XX:+HeapDumpOnOutOfMemoryError
    -Dspring.profiles.active=local
```

**GC 선택 기준:**
- ZGC: low-latency 우선 (JVM 25 기본 지원), tail latency 감소에 효과적
- G1GC: 처리량/지연 균형 (구 버전 호환성 필요 시)

### Step 6: Redis 사용 패턴 최적화

`storage/redis/` 모듈의 Redis 설정을 확인한다:

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 50    # HikariCP와 동일 수준
          max-idle: 20
          min-idle: 5
```

**배치 처리 패턴** — 여러 Redis 명령을 pipeline으로 묶으면 RTT를 줄일 수 있다:
```kotlin
redisTemplate.executePipelined {
    // 여러 Redis 명령을 한 번에 전송
}
```

### Step 7: 트랜잭션 범위 축소

`@Transactional` 범위를 최소화한다:
- 쿠폰 유효성 검증은 트랜잭션 밖에서
- Redis 재고 차감은 트랜잭션 밖에서 (분리 가능 시)
- DB INSERT만 트랜잭션 내에서

### Step 8: 변경 내역 문서화

`_workspace/02_perf_tuning_summary.md`에 기록한다:
- 변경 전후 설정값
- 계산 근거
- 예상 처리량 향상
- 부하테스트로 검증할 항목
