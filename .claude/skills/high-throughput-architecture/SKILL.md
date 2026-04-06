---
name: high-throughput-architecture
description: "초당 10,000건 이상의 쿠폰 발급 요청을 처리하는 고처리량 시스템 아키텍처를 설계하는 스킬. MQ 선택(Kafka/RabbitMQ), 동기/비동기 플로우 분리 경계, 병목 예측, 컴포넌트 구성, 단계별 로드맵을 문서화한다. '10k RPS', '대규모 처리', 'MQ 도입 설계', '아키텍처 로드맵', '비동기 플로우 설계' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# High-Throughput Architecture

단일 Spring Boot 인스턴스에서 10,000 RPS 쿠폰 발급을 달성하기 위한 아키텍처를 설계한다.

## 설계 절차

### Step 1: 현재 시스템 분석

다음 파일들을 읽어 현재 아키텍처를 파악한다:
- `settings.gradle.kts` — 모듈 구조
- `coupon/coupon-domain/src/main/kotlin/com/coupon/coupon/CouponIssueService.kt` — 발급 핵심 로직
- `coupon/coupon-domain/src/main/kotlin/com/coupon/support/outbox/OutboxEventService.kt` — Outbox 패턴
- `coupon/coupon-domain/src/main/kotlin/com/coupon/support/lock/LockRepository.kt` — 분산 락
- `coupon/coupon-api/src/main/kotlin/com.coupon/config/AsyncConfig.kt` — 비동기 설정
- `docker/docker-compose.yml` — 인프라 구성
- `load-test/k6/RUNBOOK.md` — 현재 부하테스트 기준

### Step 2: 병목 지점 예측

10k RPS 달성을 막는 병목을 계층별로 분석한다:

**DB 계층:**
- 쿠폰 발급 시 트랜잭션 내 락 대기 시간
- HikariCP 풀 크기 대비 동시 요청 수
- INSERT/UPDATE 경합 (CouponIssue 테이블)

**Redis 계층:**
- 분산 락 획득/해제 RTT (네트워크 왕복 시간)
- 재고 차감 Lua 스크립트 직렬화 처리량

**JVM/Tomcat 계층:**
- 스레드 풀 포화 (동기 Tomcat 스레드 모델의 한계)
- GC pause가 tail latency에 미치는 영향

**애플리케이션 계층:**
- Outbox 이벤트 INSERT가 발급 트랜잭션에 포함되는 비용
- 동기 처리로는 단일 인스턴스 한계가 ~2-5k RPS임

### Step 3: MQ 선택

Kafka와 RabbitMQ의 트레이드오프를 이 프로젝트 기준으로 분석한다:

| 기준 | Kafka | RabbitMQ |
|------|-------|---------|
| 처리량 | 매우 높음 (100k+/s) | 높음 (20-50k/s) |
| 지연시간 | 배치 특성상 ms 단위 | 낮음 (sub-ms 가능) |
| 순서 보장 | 파티션 내 보장 | 큐 내 보장 |
| 운영 복잡도 | 높음 (Zookeeper 또는 KRaft) | 낮음 |
| 재처리 | Offset으로 쉬움 | DLQ 활용 |
| Docker 편의성 | bitnami/kafka (KRaft) | 단순 |

**쿠폰 시스템 추천 기준:**
- 순서가 중요하고 재처리가 필요하면 → Kafka
- 빠른 도입, 낮은 운영 복잡도 → RabbitMQ
- 향후 이벤트 소싱 확장 계획 → Kafka

### Step 4: 비동기 플로우 설계

쿠폰 발급 플로우를 동기/비동기로 분리한다:

**현재 동기 플로우:**
```
Client → API → CouponIssueService → (DB 락 + INSERT) → Outbox → Response
```

**목표 비동기 플로우:**
```
Client → API → Redis (즉시 재고 선점) → MQ 발행 → Response(202 Accepted)
                                          ↓
                               Consumer → DB INSERT → Outbox → 완료
```

**설계 결정 포인트:**
- 재고 선점은 Redis atomic operation으로 (응답 지연 < 1ms)
- DB 영속화는 Consumer에서 비동기로
- 클라이언트는 polling 또는 WebSocket으로 결과 확인
- 실패 시 Redis 재고 복구 (Consumer DLQ 처리)

### Step 5: 아키텍처 결정 기록(ADR) 작성

각 팀원이 참조할 ADR을 `_workspace/00_adr.md`에 작성한다:
- MQ 선택과 이유
- 비동기 경계 위치
- mq-integrator 구현 지시사항
- perf-optimizer 최적화 목표 (목표 풀 크기, 스레드 수)
- loadtest-engineer 시나리오 목표 (VUS, thresholds)
- observability-engineer 핵심 메트릭 목록

## 출력

- `_workspace/00_architecture_decision.md`
- `_workspace/00_adr.md`

참조 자료: `references/throughput-calculation.md`
