# Article Template

## 1. Planner 템플릿

```md
제목 후보:
- ...
- ...

독자:
- ...

한 줄 주장:
- ...

비교할 대안:
- ...

개요:
- 문제 제기
- 현재 구조
- 선택 이유
- 실패 처리 또는 운영 포인트
- 트레이드오프
- 결론

섹션별 근거:
- 문제 제기: ...
- 현재 구조: ...
- 선택 이유: ...

이미지 제안:
- 구간: ...
  - 유형: sequence diagram | architecture diagram | comparison table | flowchart | metric graph | before/after diagram
  - 이유: ...
  - 캡션 초안: ...

References 초안:
- Local References
  - [R1] ...
- External References
  - [R2] ...
- Writing Benchmarks
  - [B1] ...
```

## 2. Drafter 출력 템플릿

```md
개요:
- 문제 제기
- 현재 구조
- 선택 이유
- 실패 처리 또는 운영 포인트
- 트레이드오프
- 결론

초안:
# 제목
## 부제
## 목차
## 서론
## 현재 구조
## 왜 이 선택을 했는가
## 실패와 운영에서 무엇이 달라지는가
## 한계와 다음 단계
## 마무리

이미지 제안:
- 구간: ...
  - 유형: ...
  - 이유: ...
  - 캡션 초안: ...

References:
### Local References
- [R1] ...

### External References
- [R2] ...
- [R3] ...

### Writing Benchmarks
- [B1] ...
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
- 본문 하단 `References` 는 필수다
- factual claim 이 많은 문단은 필요하면 `[R1]`, `[R2]` 를 붙여도 된다
