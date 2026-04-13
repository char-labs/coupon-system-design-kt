# Article Template

## 1. Planner 템플릿

```md
제목 후보:
- ...
- ...
- ...

독자:
- ...

한 줄 주장:
- ...

도입 훅:
- 독자가 바로 공감할 문제 한 줄
- 필요하면 짧은 비유 한 줄
- 가능하면 `이번 프로젝트에서 제가 처음 부딪힌 문제는 ...` 같은 직접 경험 문장으로 시작하기

헤드라인 메모:
- 구체 기술명 또는 설계 선택을 제목에 넣기
- 가능하면 `사용기`, `과정`, `에 대해`, `왜 ... 인가` 중 하나를 활용하기
- 추상적인 카피보다 글 범위가 보이게 쓰기

비교할 대안:
- ...

개요:
- 계기 또는 문제 제기
- 현재 구조
- 선택 이유
- 예상치 못한 이슈 또는 헷갈리기 쉬운 지점
- 실패 처리 또는 운영 포인트
- 트레이드오프
- 후기 또는 결론

상세 설명이 특히 필요한 구간:
- 독자가 쉽게 오해할 수 있는 런타임 경계
- 기존 대안과 현재 선택의 차이
- 실패 시 어떤 보상 또는 재시도가 일어나는지
- 왜 단순한 방법을 쓰지 않았는지
- 주제가 Redis Lua면 `reserve / release / rebuild` 각각의 역할과 호출 시점

섹션별 근거:
- 문제 제기: ...
- 현재 구조: ...
- 선택 이유: ...

이미지 제안:
- 구간: ...
  - 유형: sequence diagram | architecture diagram | comparison table | flowchart | metric graph | before/after diagram
  - 이유: ...
  - 캡션 초안: ...

근거 팩 초안:
- External References
  - [R2] ...
- Writing Benchmarks
  - [B1] ...

publish 메모:
- 로컬 근거를 본문 어디에 어떤 코드 컨텍스트 블록이나 스니펫으로 녹일지
- publish용 초안에 남길 외부 References 범위
- frontmatter와 대표 이미지 적용 여부
- inline citation anchor를 쓸지, plain 링크를 쓸지
```

## 2. Drafter 출력 템플릿

```md
개요:
- 계기 또는 문제 제기
- 현재 구조
- 선택 이유
- 예상치 못한 이슈 또는 헷갈리기 쉬운 지점
- 실패 처리 또는 운영 포인트
- 트레이드오프
- 후기 또는 결론

초안:
---
title: "..."
description: "..."
date: "YYYY-MM-DD"
tags:
  - ...
  - ...
---

<img src="..." width="100%" />

# 제목
## 서론
## 왜 이 글을 쓰게 되었는가
## 개념 설명
## 구조 고정
## 핵심 함수 설명
## 기본 대안 비교
## 현재 코드 설명
## 실패와 복구
## 헷갈리기 쉬운 지점
## 최종 결정
## 결론

이미지 제안:
- 구간: ...
  - 유형: ...
  - 이유: ...
  - 캡션 초안: ...

근거 팩:
### External References
- [R2] ...
- [R3] ...

### Writing Benchmarks
- [B1] ...
```

publish용 `초안` 안에는 기본적으로 `### Local References` 같은 내부 섹션을 넣지 않는다.
로컬 근거는 아래 예시처럼 코드 컨텍스트 블록이나 짧은 코드 스니펫으로 바꿔 넣는다.

```md
> 코드 컨텍스트
> - `CouponIssueIntakeFacade` 는 Redis reserve 성공 뒤 Kafka publish 를 시도합니다
> - publish 실패 시 `release` 로 선점 상태를 되돌립니다
> - `CouponIssueRedisCoreRepository` 는 reserve/release/rebuild 를 같은 경계에 모읍니다
```

또는 실제 teaching point 가 코드라면 짧은 스니펫을 둔다.

```kotlin
val issueResult = couponIssueRedisCoreRepository.reserve(couponId, userId, quantity, ttl)
if (issueResult == CouponIssueResult.SUCCESS) {
    couponIssueProducer.publish(command)
}
```

외부 근거를 본문 중간에 연결해야 하면 아래처럼 anchor 기반 표기를 허용한다.

```md
Redis에서 Lua 스크립트는 Redis 서버 안에서 실행되는 짧은 사용자 정의 로직입니다.
<a href="#ref-e1">[E1]</a> <a href="#ref-e2">[E2]</a>
```

최종 `References` 는 아래처럼 맞춘다.

```md
## References

### External References

- <span id="ref-e1">[E1]</span> [Redis Docs: Scripting with Lua](https://redis.io/docs/latest/develop/programmability/eval-intro/)
- <span id="ref-e2">[E2]</span> [Redis Docs: Redis programmability](https://redis.io/docs/latest/develop/programmability/)
```

## 3. 본문 내 이미지 제안 블록

```md
이미지 제안:
- 구간: `왜 이 선택을 했는가`
- 유형: `comparison table`
- 이유: `락 / Kafka / Lua 의 책임 차이를 글보다 빠르게 전달할 수 있다`
- 캡션 초안: `표 1. 현재 저장소에서 락, Kafka, Redis Lua 가 맡는 책임 비교`
```

## 4. Reviewer 피드백 템플릿

```md
치명적 사실 오류:
- ...

보강 필요:
- ...

이미지 제안 조정:
- ...

레퍼런스 누락 또는 약한 근거:
- ...

최종 반영 메모:
- ...
```

## 작성 규칙

- `현재 구현 사실` 과 `미래 TODO` 를 섞지 않는다
- `SUCCESS` 의미와 outbox 범위를 항상 현재 계약 기준으로 쓴다
- 일반론보다 현재 코드와 문서를 먼저 설명한다
- publish용 MDX라면 frontmatter를 기본 포함한다
- frontmatter의 `description` 은 글의 핵심 선택 이유와 현재 프로젝트 맥락이 한 문장에 드러나게 쓴다
- 대표 이미지가 있다면 frontmatter 바로 아래에 둔다
- 대표 이미지는 글의 주제를 바로 인지시키는 용도여야 하고, 장식용이면 생략한다
- `## 서론` 섹션을 명시적으로 두는 패턴을 허용한다
- 본문 서술 기본형은 `합니다체` 입니다
- 직접 인용이나 정의문이 아니면 `한다체` 는 피한다
- 서론 한두 문단은 친근한 1인칭 또는 문제 공감 톤을 허용한다
- 최근 작성자 글처럼 `왜 이 글을 쓰는지` 를 초반에 드러낸다
- `백엔드 개발자 관점에서` 같은 메타 표현보다 `제가 구현하면서`, `제가 중요하게 본 점은` 같은 직접 경험형 문장을 우선한다
- publish용 초안에는 `Local References` 같은 내부 섹션을 남기지 않는다
- 로컬 사실은 코드 컨텍스트 블록, 짧은 코드 발췌, 흐름 설명으로 변환한다
- 짧게 보이기 위해 설명을 잘라내지 않는다
- 어려운 구간은 예시, 비교, 실패 시나리오, 운영 관점까지 확장해서 설명해도 된다
- 길이는 `짧음` 보다 `이해 가능성` 에 맞춘다
- Mermaid 다이어그램은 기본적으로 한국어 의미 라벨을 쓰고, raw 명령명은 코드 블록이나 본문에서 설명한다
- 비유는 글당 1~2개 이하로 제한한다
- 비유 다음 문단에서는 반드시 기술 설명으로 바로 회수한다
- 외부 `근거 팩` 은 필수다
- factual claim 이 많은 문단은 필요하면 `[R1]`, `[R2]` 를 붙여도 된다
- publish 플랫폼이 anchor 링크를 잘 지원하면 `<a href="#ref-e1">[E1]</a>` 와 `<span id="ref-e1">[E1]</span>` 패턴을 사용할 수 있다
