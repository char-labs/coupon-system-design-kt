# 처리량 계산 참조

## 리틀의 법칙 (Little's Law)

```
처리량(L) = 동시 요청 수(λ) × 평균 응답시간(W)
```

**예시:**
- 목표: 10,000 RPS
- 평균 응답시간: 10ms
- 필요 동시 처리 수: 10,000 × 0.01 = **100 concurrent requests**

## HikariCP 풀 크기 공식

```
pool_size = (core_count × 2) + effective_spindle_count
```

**단일 인스턴스 기준 (4코어 가정):**
- 권장 풀: (4 × 2) + 1 = 9~20개
- 10k RPS + 10ms DB 응답시간 → 최소 100개 필요
- 실제 DB 처리량 한계를 먼저 측정할 것

## Tomcat 스레드 수

- 동기 모델: 스레드 1개 = 요청 1개
- 10k RPS + 10ms 처리 = 100개 스레드 필요
- `server.tomcat.threads.max=200` 이상 설정 권장

## Kafka 파티션 수

```
파티션 수 = 목표처리량 / (단일 Consumer 처리량)
```

- Consumer 1개가 초당 1,000건 처리 가능 가정
- 10k RPS → 10개 파티션

## Redis 분산 락 처리량

- Redis single-threaded: 초당 약 100,000 SET 명령
- 락 획득/해제 = 2 RTT
- 로컬 Redis RTT ≈ 0.1ms → 이론상 500k/s 가능
- 실제 네트워크 + Lua 스크립트 감안 시 10-50k/s 수준
