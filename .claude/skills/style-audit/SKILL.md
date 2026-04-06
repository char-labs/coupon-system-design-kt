---
name: style-audit
description: "Kotlin/Spring Boot 프로젝트의 코드 스타일과 품질을 감사하는 스킬. Kotlin 관용구 활용, 네이밍 일관성, 코드 중복, 테스트 품질, 에러 처리 패턴을 분석한다. '코드 스타일 리뷰', '코드 품질 분석', 'Kotlin 관용구 검토', '테스트 품질 평가', '네이밍 검토' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# Style Audit

Kotlin/Spring Boot 프로젝트의 코드 스타일과 품질을 체계적으로 감사한다.

## 감사 절차

### Step 1: Kotlin 관용구 활용

ktlint가 잡지 못하는 Kotlin 스타일 이슈를 찾는다:
- `data class` 활용도 — DTO/값 객체에 일반 class 사용 여부
- `sealed class/interface` — 타입 분기 시 when 완전성 보장 여부
- `extension function` — 유틸리티 메서드가 companion object에 과도하게 모여 있는 경우
- scope function (`let`, `run`, `apply`, `also`) 오용 또는 미활용
- `require`/`check`/`error` — 전제조건 검증에 if-throw 대신 표준 함수 활용 여부
- nullable 처리 — `!!` 남용, 불필요한 nullable 타입

### Step 2: 네이밍 일관성

프로젝트 전체의 네이밍 패턴을 분석한다:
- 클래스명: `Service`, `Repository`, `Controller`, `Facade` 등 접미사 일관성
- 함수명: CRUD 동사 일관성 (`create`/`save`/`register` 혼용 여부)
- 변수명: 약어 사용 일관성, 의미 전달력
- 패키지 구조: 도메인 기반 vs 기술 기반 혼용 여부

### Step 3: 코드 중복

유사한 로직의 반복을 탐지한다:
- 여러 서비스에서 반복되는 검증 로직
- 유사한 변환(mapping) 로직
- 테스트 코드의 불필요한 반복 (fixture로 추출 가능)

### Step 4: 테스트 품질

테스트 코드의 구조와 품질을 평가한다:
- 테스트 네이밍: 행위를 명확히 표현하는가?
- Given-When-Then 구조 일관성
- Fixture 활용도 (kotlin-fixture, Instancio 등)
- MockK/Mockito 사용 일관성
- 경계값, 에러 케이스 테스트 존재 여부
- Kotest 스타일 일관성 (BehaviorSpec, FunSpec 등)

### Step 5: 에러 처리

예외 처리 패턴의 일관성을 검증한다:
- 커스텀 예외 계층 구조
- 예외 메시지의 정보 충분성
- 예외 삼키기(catch 후 무시) 여부
- API 에러 응답 형식 일관성

## 심각도 기준

| 심각도 | 기준 |
|--------|------|
| Warning | 가독성/유지보수성에 직접 영향을 주는 이슈 |
| Info | 개선하면 좋지만 기능에 영향 없는 이슈 |
| Suggestion | 선택적 개선 사항 |

## 출력

결과를 `_workspace/04_style_audit.md`에 저장한다. 각 이슈에는 파일 경로, 라인 번호, 개선 예시 코드를 포함한다.
