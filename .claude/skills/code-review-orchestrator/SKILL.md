---
name: code-review-orchestrator
description: "종합 코드 리뷰를 수행하는 에이전트 팀 오케스트레이터. 아키텍처, 보안, 성능, 코드 스타일 4개 영역을 병렬 감사하고 통합 리포트를 생성한다. '코드 리뷰', '종합 리뷰', '코드 감사', '전체 코드 점검', '리뷰 리포트', 'code review' 등의 요청 시 반드시 이 스킬을 사용할 것. 단일 영역(아키텍처만, 보안만 등)이 아닌 종합 리뷰를 원할 때 트리거된다."
---

# Code Review Orchestrator

4명의 전문 감사 에이전트를 팬아웃/팬인 패턴으로 조율하여 종합 코드 리뷰 리포트를 생성한다.

## 실행 모드: 에이전트 팀

## 에이전트 구성

| 팀원 | 에이전트 타입 | 역할 | 스킬 | 출력 |
|------|-------------|------|------|------|
| architecture-auditor | Explore | 모듈 구조, 의존성, 레이어 분리 | architecture-audit | `_workspace/01_architecture_audit.md` |
| security-auditor | Explore | OWASP 기반 보안 취약점 | security-audit | `_workspace/02_security_audit.md` |
| performance-auditor | Explore | DB, 동시성, 캐싱, 리소스 | performance-audit | `_workspace/03_performance_audit.md` |
| style-auditor | Explore | Kotlin 관용구, 네이밍, 테스트 | style-audit | `_workspace/04_style_audit.md` |

## 워크플로우

### Phase 1: 준비

1. 사용자 입력에서 감사 범위를 파악한다:
   - 전체 프로젝트 vs 특정 모듈 vs 특정 변경사항(diff)
   - 특별히 주의할 영역이 있는지 확인
2. `_workspace/` 디렉토리를 생성한다
3. 감사 범위를 `_workspace/00_scope.md`에 기록한다

### Phase 2: 팀 구성

1. 팀을 생성한다:
   ```
   TeamCreate(
     team_name: "code-review-team",
     members: [
       {
         name: "architecture-auditor",
         agent_type: "Explore",
         model: "sonnet",
         prompt: ".claude/agents/architecture-auditor.md의 역할에 따라 프로젝트를 감사하라. 스킬은 .claude/skills/architecture-audit/SKILL.md를 참조하라. 감사 범위는 _workspace/00_scope.md를 읽어라. 결과를 _workspace/01_architecture_audit.md에 저장하라. 보안/성능 관련 발견은 해당 팀원에게 SendMessage로 공유하라."
       },
       {
         name: "security-auditor",
         agent_type: "Explore",
         model: "sonnet",
         prompt: ".claude/agents/security-auditor.md의 역할에 따라 프로젝트를 감사하라. 스킬은 .claude/skills/security-audit/SKILL.md를 참조하라. 감사 범위는 _workspace/00_scope.md를 읽어라. 결과를 _workspace/02_security_audit.md에 저장하라. 아키텍처/성능 팀원의 발견을 수신하고 보안 관점으로 보완하라."
       },
       {
         name: "performance-auditor",
         agent_type: "Explore",
         model: "sonnet",
         prompt: ".claude/agents/performance-auditor.md의 역할에 따라 프로젝트를 감사하라. 스킬은 .claude/skills/performance-audit/SKILL.md를 참조하라. 감사 범위는 _workspace/00_scope.md를 읽어라. 결과를 _workspace/03_performance_audit.md에 저장하라. 아키텍처 팀원의 구조적 성능 발견을 수신하라."
       },
       {
         name: "style-auditor",
         agent_type: "Explore",
         model: "sonnet",
         prompt: ".claude/agents/style-auditor.md의 역할에 따라 프로젝트를 감사하라. 스킬은 .claude/skills/style-audit/SKILL.md를 참조하라. 감사 범위는 _workspace/00_scope.md를 읽어라. 결과를 _workspace/04_style_audit.md에 저장하라. 성능 팀원이 성능 관련 코드 패턴을 공유하면 반영하라."
       }
     ]
   )
   ```

2. 작업을 등록한다:
   ```
   TaskCreate(tasks: [
     { title: "아키텍처 감사", description: "모듈 의존성, 레이어 분리, 도메인 설계, 패턴 일관성 분석", assignee: "architecture-auditor" },
     { title: "보안 감사", description: "OWASP Top 10 기준 인증/인가, 입력 검증, 데이터 보호, 설정 보안 분석", assignee: "security-auditor" },
     { title: "성능 감사", description: "DB 접근, 동시성 제어, 캐싱, 트랜잭션, 리소스 사용 분석", assignee: "performance-auditor" },
     { title: "스타일 감사", description: "Kotlin 관용구, 네이밍, 중복, 테스트 품질, 에러 처리 분석", assignee: "style-auditor" }
   ])
   ```

### Phase 3: 병렬 감사 수행

**실행 방식:** 4명의 팀원이 독립적으로 감사 수행, 교차 발견 시 팀원 간 직접 공유

**팀원 간 통신 규칙:**
- architecture-auditor → security-auditor: 인증 레이어 우회 가능 경로 발견 시
- architecture-auditor → performance-auditor: 구조적 성능 문제 (N+1 유발 구조 등) 발견 시
- performance-auditor → style-auditor: 성능에 영향을 주는 코드 패턴 발견 시
- security-auditor → performance-auditor: 보안-성능 트레이드오프 발견 시

**산출물 저장:**

| 팀원 | 출력 경로 |
|------|----------|
| architecture-auditor | `_workspace/01_architecture_audit.md` |
| security-auditor | `_workspace/02_security_audit.md` |
| performance-auditor | `_workspace/03_performance_audit.md` |
| style-auditor | `_workspace/04_style_audit.md` |

**리더 모니터링:**
- 팀원 유휴 알림 수신 시 다음 팀원의 진행 상태 확인
- 전체 진행률은 TaskGet으로 확인
- 특정 팀원이 막혔으면 SendMessage로 범위를 좁혀 지시

### Phase 4: 통합 리포트 생성

모든 팀원의 작업 완료 후:

1. 4개 산출물을 Read로 수집한다
2. 중복 이슈를 병합한다 (같은 파일/라인에 대한 다른 관점의 이슈)
3. 교차 영역 이슈를 식별한다 (아키텍처 문제가 보안 취약점으로 이어지는 경우 등)
4. 통합 리포트를 생성한다

**통합 리포트 형식:**

```markdown
# Comprehensive Code Review Report

**프로젝트:** {project-name}
**감사 범위:** {scope}
**감사 일시:** {date}

## Executive Summary
- 전체 이슈 수 (Critical / High / Warning / Info)
- 가장 긴급한 조치 사항 Top 3
- 전반적 코드 품질 평가 (A~E 등급)

## Critical & High Issues
모든 심각한 이슈를 영역 구분 없이 심각도순으로 나열

## Architecture
01_architecture_audit.md의 주요 발견 요약

## Security
02_security_audit.md의 주요 발견 요약

## Performance
03_performance_audit.md의 주요 발견 요약

## Code Style & Quality
04_style_audit.md의 주요 발견 요약

## Cross-cutting Concerns
여러 영역에 걸치는 이슈

## Action Items
우선순위별 조치 사항 체크리스트
- [ ] Critical: ...
- [ ] High: ...
- [ ] Warning: ...
```

5. 통합 리포트를 `_workspace/05_final_report.md`에 저장한다
6. 사용자에게 결과 요약을 보고한다

### Phase 5: 정리

1. 팀원들에게 종료 요청 (SendMessage)
2. 팀 정리 (TeamDelete)
3. `_workspace/` 디렉토리 보존 (사후 검증용)
4. 사용자에게 `_workspace/05_final_report.md` 경로와 요약 보고

## 에러 핸들링

| 에러 유형 | 전략 |
|----------|------|
| 팀원 응답 없음 | 1회 SendMessage로 재촉 → 재실패 시 해당 영역 "감사 미완료"로 표시 |
| 산출물 파일 없음 | 팀원에게 상태 확인 → 부분 결과라도 수집 |
| 상충 발견 | 두 관점 모두 리포트에 포함, 출처 병기 |
| 감사 범위 과대 | 핵심 모듈(coupon-domain, coupon-api) 우선 감사 지시 |

## 테스트 시나리오

### 정상 흐름
1. 사용자: "전체 코드 리뷰해줘"
2. 오케스트레이터: 범위를 전체 프로젝트로 설정, 팀 구성, 4명 병렬 감사
3. 각 팀원이 산출물 저장, 교차 발견 공유
4. 리더가 통합 리포트 생성
5. 사용자에게 결과 보고

### 에러 흐름
1. 사용자: "coupon-domain만 리뷰해줘"
2. 오케스트레이터: 범위를 coupon-domain으로 한정
3. performance-auditor가 k6 파일을 찾지 못함 → 해당 섹션 건너뜀
4. 나머지 3명 정상 완료
5. 통합 리포트에 "부하 테스트 정합성: k6 파일 없음으로 미분석" 명시
