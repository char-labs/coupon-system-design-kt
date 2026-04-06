---
name: highscale-coupon-orchestrator
description: "10k RPS 쿠폰 시스템 구축을 위한 에이전트 팀 오케스트레이터. system-architect의 설계를 바탕으로 MQ 통합, JVM 성능 최적화, 부하테스트 확장, 옵저버빌리티 강화를 병렬로 수행한다. '10k RPS 구축', '대규모 쿠폰 시스템', 'Kafka 도입', 'RabbitMQ 도입', '부하테스트 런북', '메시지 큐 쿠폰', '고처리량 쿠폰' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# High-Scale Coupon System Orchestrator

10k RPS 단일 인스턴스 쿠폰 시스템 구축을 위해 5명의 전문 에이전트를 2 Phase로 조율한다.

## 실행 모드: 에이전트 팀 (파이프라인 + 팬아웃)

```
Phase 1 팀: system-architect (설계 문서 작성)
     ↓ (TeamDelete → TeamCreate)
Phase 2 팀: mq-integrator | perf-optimizer | loadtest-engineer | observability-engineer (병렬)
     ↓
오케스트레이터: 통합 요약 문서 생성
```

## 에이전트 구성

| 팀원 | 에이전트 타입 | Phase | 스킬 | 출력 |
|------|-------------|-------|------|------|
| system-architect | general-purpose | 1 | high-throughput-architecture | `_workspace/00_adr.md` |
| mq-integrator | general-purpose | 2 | mq-integration | Docker/Spring MQ 코드 |
| perf-optimizer | general-purpose | 2 | jvm-performance | 설정 파일 수정 |
| loadtest-engineer | general-purpose | 2 | k6-scenario-builder | k6 시나리오 + RUNBOOK |
| observability-engineer | general-purpose | 2 | metrics-observability | Prometheus + Grafana |

## 워크플로우

### Phase 1: 준비

1. `_workspace/` 디렉토리를 생성한다
2. 사용자 요청에서 다음을 파악한다:
   - MQ 선택 선호 (Kafka / RabbitMQ / 미결정)
   - 우선순위 영역 (MQ 먼저 / 성능 먼저 / 전체)
   - 현재 목표 처리량
3. `_workspace/00_context.md`에 사용자 요청과 컨텍스트를 기록한다

### Phase 2: 아키텍처 설계 (Phase 1 팀)

1. 팀을 구성한다:
   ```
   TeamCreate(
     team_name: "design-team",
     members: [
       {
         name: "system-architect",
         agent_type: "general-purpose",
         model: "sonnet",
         prompt: ".claude/agents/system-architect.md의 역할에 따라 아키텍처를 설계하라. 스킬은 .claude/skills/high-throughput-architecture/SKILL.md를 참조하라. _workspace/00_context.md를 읽고 사용자 요구사항을 파악하라. 현재 코드베이스(settings.gradle.kts, CouponIssueService.kt, AsyncConfig.kt, docker-compose.yml)를 읽어 현황을 파악하라. 결과를 _workspace/00_architecture_decision.md와 _workspace/00_adr.md에 저장하라."
       }
     ]
   )
   ```

2. 작업을 등록한다:
   ```
   TaskCreate(tasks: [
     {
       title: "아키텍처 설계 및 ADR 작성",
       description: "10k RPS 달성을 위한 MQ 선택, 비동기 플로우, 병목 예측, 각 팀원 지시사항을 담은 ADR 작성",
       assignee: "system-architect"
     }
   ])
   ```

3. system-architect 완료 대기 (유휴 알림 수신)
4. `_workspace/00_adr.md` 존재 확인

5. 팀 정리:
   ```
   TeamDelete("design-team")
   ```

### Phase 3: 병렬 구현 (Phase 2 팀)

`_workspace/00_adr.md`의 결정을 바탕으로 4명이 병렬 작업한다:

1. 팀을 구성한다:
   ```
   TeamCreate(
     team_name: "implementation-team",
     members: [
       {
         name: "mq-integrator",
         agent_type: "general-purpose",
         model: "sonnet",
         prompt: ".claude/agents/mq-integrator.md의 역할에 따라 MQ를 통합하라. 스킬은 .claude/skills/mq-integration/SKILL.md를 참조하라. 반드시 _workspace/00_adr.md를 먼저 읽어 architect의 결정을 따르라. 완료 시 _workspace/01_mq_integration_summary.md를 저장하고 loadtest-engineer와 observability-engineer에게 SendMessage로 완료를 알려라."
       },
       {
         name: "perf-optimizer",
         agent_type: "general-purpose",
         model: "sonnet",
         prompt: ".claude/agents/perf-optimizer.md의 역할에 따라 성능을 최적화하라. 스킬은 .claude/skills/jvm-performance/SKILL.md를 참조하라. 반드시 _workspace/00_adr.md를 먼저 읽어라. 완료 시 _workspace/02_perf_tuning_summary.md를 저장하고 loadtest-engineer에게 SendMessage로 검증할 파라미터를 전달하라."
       },
       {
         name: "loadtest-engineer",
         agent_type: "general-purpose",
         model: "sonnet",
         prompt: ".claude/agents/loadtest-engineer.md의 역할에 따라 k6 시나리오를 작성하라. 스킬은 .claude/skills/k6-scenario-builder/SKILL.md를 참조하라. _workspace/00_adr.md를 먼저 읽어라. mq-integrator와 perf-optimizer의 완료 알림을 받으면 해당 _workspace 파일을 읽어 시나리오에 반영하라. 완료 시 _workspace/03_loadtest_summary.md를 저장하라."
       },
       {
         name: "observability-engineer",
         agent_type: "general-purpose",
         model: "sonnet",
         prompt: ".claude/agents/observability-engineer.md의 역할에 따라 메트릭 대시보드를 구성하라. 스킬은 .claude/skills/metrics-observability/SKILL.md를 참조하라. _workspace/00_adr.md를 먼저 읽어라. mq-integrator 완료 알림을 받으면 MQ 메트릭 포인트를 반영하라. 완료 시 _workspace/04_observability_summary.md를 저장하라."
       }
     ]
   )
   ```

2. 작업을 등록한다:
   ```
   TaskCreate(tasks: [
     { title: "MQ 통합 구현", description: "Docker Compose, Spring 설정, Outbox 브릿지, Consumer, DLQ 구현", assignee: "mq-integrator" },
     { title: "JVM/Spring 성능 최적화", description: "HikariCP, Tomcat, AsyncConfig, JVM 플래그, Redis 풀 조정", assignee: "perf-optimizer" },
     { title: "k6 고처리량 시나리오 작성", description: "10k RPS 시나리오, MQ 비동기 시나리오, RUNBOOK 업데이트", assignee: "loadtest-engineer" },
     { title: "Prometheus + Grafana 통합", description: "앱 메트릭 대시보드, MQ 메트릭, 병목 탐지 패널 구성", assignee: "observability-engineer" }
   ])
   ```

3. **팀원 간 통신 규칙:**
   - mq-integrator → loadtest-engineer: MQ 엔드포인트, 비동기 플로우 확인 방법
   - mq-integrator → observability-engineer: MQ 메트릭 수집 포인트
   - perf-optimizer → loadtest-engineer: 검증할 설정 파라미터 목록
   - perf-optimizer → observability-engineer: 모니터링할 핵심 메트릭

4. 모든 팀원 완료 대기 (TaskGet으로 확인)

5. 팀 정리:
   ```
   TeamDelete("implementation-team")
   ```

### Phase 4: 통합 요약 생성

4개 산출물을 Read로 수집하여 `_workspace/99_project_summary.md`를 생성한다:

```markdown
# 10k RPS 쿠폰 시스템 구축 요약

## 아키텍처 결정
(00_adr.md 요약)

## 구현 내역
### MQ 통합 (01_mq_integration_summary.md 요약)
### 성능 최적화 (02_perf_tuning_summary.md 요약)

## 부하테스트 실행 방법
(03_loadtest_summary.md 핵심 명령어)

## 모니터링 방법
(04_observability_summary.md 핵심 대시보드 접근법)

## 다음 단계
- 10k RPS 부하테스트 실행 결과 확인
- 병목 발생 시 조치 순서
- 단계별 확장 로드맵
```

### Phase 5: 정리

1. `_workspace/` 보존
2. 사용자에게 결과 보고:
   - 변경된 파일 목록
   - 부하테스트 실행 명령어 (복사해서 바로 실행 가능하게)
   - Grafana 대시보드 접근 방법

## 에러 핸들링

| 에러 유형 | 전략 |
|----------|------|
| mq-integrator 구현 실패 | Docker 설정만이라도 완료 후 나머지 TODO 문서화 |
| perf-optimizer 설정 충돌 | 충돌 내용을 `_workspace/02_perf_conflicts.md`에 기록 |
| loadtest-engineer MQ 대상 없음 | 스켈레톤 시나리오 + TODO 주석으로 완료 |
| observability-engineer Grafana 충돌 | 기존 대시보드 보존, 신규 대시보드는 별도 추가 |

## 테스트 시나리오

### 정상 흐름
1. "10k RPS 쿠폰 시스템 구축해줘, Kafka 선호해" 요청
2. Phase 1: system-architect가 Kafka 기반 ADR 작성 (30분)
3. Phase 2: 4명 병렬 구현 — mq-integrator(Kafka), perf-optimizer(HikariCP+Tomcat), loadtest-engineer(ramping-arrival-rate 시나리오), observability-engineer(Prometheus+Grafana)
4. 통합 요약 생성
5. 사용자에게 "docker compose up 하고 이 명령어로 부하테스트 실행하세요" 보고

### 에러 흐름
1. "MQ는 나중에, 지금은 단순히 10k RPS 달성하는 JVM 튜닝부터" 요청
2. Phase 1: architect가 MQ 없이 JVM/캐싱 중심 ADR 작성
3. Phase 2: perf-optimizer(JVM 튜닝), loadtest-engineer(시나리오) 우선, mq-integrator는 설계 문서만, observability-engineer(메트릭 대시보드)
4. 통합 요약: MQ 관련 항목은 "미구현 — 이후 단계 예정"으로 표시
