---
name: security-audit
description: "Kotlin/Spring Boot 프로젝트의 보안 취약점을 OWASP Top 10 기준으로 감사하는 스킬. 인증/인가 흐름, 입력 검증, 민감 데이터 노출, Redis 토큰 보안, 의존성 취약점을 분석한다. '보안 점검', '취약점 분석', 'OWASP 감사', '인증 검증' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# Security Audit

Kotlin/Spring Boot 프로젝트의 보안 취약점을 OWASP Top 10 기준으로 체계적으로 감사한다.

## 감사 절차

### Step 1: 인증/인가 흐름 추적

1. 인증 진입점(Controller)부터 토큰 발급까지의 전체 흐름을 추적한다
2. 인가 검증이 적용되지 않은 엔드포인트를 찾는다
3. 토큰 생성/검증/만료 로직의 안전성을 평가한다
4. Redis 토큰 저장의 TTL, 무효화 메커니즘을 확인한다

### Step 2: 입력 검증

API 엔드포인트의 입력 처리를 검사한다:
- Request DTO에 `@Valid`, `@NotNull` 등 검증 어노테이션이 적용되는가?
- 사용자 입력이 쿼리에 직접 사용되는가? (SQL/NoSQL Injection)
- Path Variable, Query Parameter의 타입 안전성
- 대량 요청 방어(Rate Limiting) 존재 여부

### Step 3: 민감 데이터 보호

코드베이스에서 민감 데이터 처리를 검사한다:
- 비밀번호 평문 저장/로깅 여부
- 토큰/시크릿의 하드코딩 여부
- 응답에 불필요한 내부 정보 노출 (스택트레이스, DB 에러 등)
- `application.yml`/`properties`에 시크릿 평문 저장 여부

### Step 4: 설정 보안

- CORS 설정의 적정성 (와일드카드 허용 여부)
- HTTPS 강제 여부
- 보안 헤더 설정 (HSTS, X-Frame-Options 등)
- 액추에이터/디버그 엔드포인트 노출 여부

### Step 5: 의존성 보안

- `build.gradle.kts`에서 의존성 버전을 확인한다
- Spring Boot, Jackson 등 핵심 프레임워크의 알려진 취약점 여부
- 더 이상 유지보수되지 않는 라이브러리 사용 여부

### Step 6: 쿠폰 도메인 특수 보안

쿠폰 시스템 고유의 보안 위험을 점검한다:
- 쿠폰 코드 생성의 예측 가능성 (brute force 가능 여부)
- 쿠폰 발급 횟수 제한 우회 가능성
- Race condition을 이용한 중복 발급 가능성

## 심각도 기준

| 심각도 | 기준 |
|--------|------|
| Critical | 인증 우회, SQL Injection, 시크릿 노출 |
| High | 인가 누락, 민감 데이터 평문 저장 |
| Medium | 입력 검증 미흡, 보안 헤더 누락 |
| Low | 개선 권장 사항 |

## 출력

결과를 `_workspace/02_security_audit.md`에 저장한다. 각 이슈에는 파일 경로, 라인 번호, 재현 시나리오를 포함한다.
