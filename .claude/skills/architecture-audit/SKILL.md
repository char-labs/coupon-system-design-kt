---
name: architecture-audit
description: "Kotlin/Spring Boot 멀티모듈 프로젝트의 아키텍처를 감사하는 스킬. 모듈 의존성 방향, 레이어 분리, 도메인 설계, 패턴 일관성을 분석한다. '아키텍처 리뷰', '모듈 구조 분석', '의존성 검증', '레이어 위반 확인' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# Architecture Audit

Kotlin/Spring Boot 멀티모듈 프로젝트의 아키텍처를 체계적으로 감사한다.

## 감사 절차

### Step 1: 모듈 구조 파악

1. `settings.gradle.kts`에서 전체 모듈 목록을 확인한다
2. 각 모듈의 `build.gradle.kts`에서 의존성을 추출한다
3. 모듈 의존성 그래프를 구성한다

### Step 2: 의존성 방향 검증

다음 규칙 위반을 탐지한다:
- **domain → infra 의존 금지:** domain 모듈이 db-core, redis 등 인프라 모듈에 직접 의존하면 위반
- **순환 의존 탐지:** A → B → A 형태의 순환 고리
- **coupon-enum 사용 패턴:** enum 모듈이 다른 모듈에 의존하면 위반 (enum은 의존 없는 순수 모듈)

### Step 3: 레이어 분리 검증

각 모듈 내부의 레이어 경계를 확인한다:
- API 레이어(Controller)가 Repository를 직접 호출하는가?
- Domain 서비스가 HTTP/웹 관련 클래스를 import하는가?
- Facade/Service 간 역할 분리가 명확한가?

### Step 4: 도메인 설계 평가

- **응집도:** 하나의 서비스가 너무 많은 책임을 가지는가?
- **결합도:** 도메인 간 불필요한 직접 참조가 있는가?
- **값 객체:** 원시 타입 남용(Primitive Obsession)이 있는가?
- **도메인 이벤트:** 이벤트 기반 통신이 적절히 사용되는가?

### Step 5: 패턴 일관성

프로젝트에서 사용하는 주요 패턴의 일관된 적용 여부를 확인한다:
- Outbox 패턴이 모든 도메인 이벤트에 적용되는가?
- 동시성 제어 방식이 일관적인가? (비관적 락 vs 낙관적 락 혼용 여부)
- Command/Criteria 패턴이 일관되게 사용되는가?

## 심각도 기준

| 심각도 | 기준 |
|--------|------|
| Critical | 순환 의존, domain→infra 역방향 의존 |
| Warning | 레이어 경계 위반, 패턴 불일관 |
| Info | 개선 가능하지만 현재 동작에 문제 없는 사항 |

## 출력

결과를 `_workspace/01_architecture_audit.md`에 저장한다. 각 이슈에는 파일 경로와 라인 번호를 포함한다.
