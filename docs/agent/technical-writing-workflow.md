# Technical Writing Workflow

## 목적

이 문서는 현재 `coupon-system-design-kt` 저장소를 기준으로 `왜 outbox를 쓰는가`, `왜 Lua를 쓰는가`,
`왜 Kafka 로 intake 와 execution 을 분리했는가` 같은 질문형 백엔드 블로그 글을 쓰는 표준 워크플로를 정의한다.

핵심 목표는 네 가지다.

- 현재 저장소 사실에 맞는 글을 쓴다
- 외부 레퍼런스를 붙여 주장에 근거를 만든다
- 글로만 부족한 지점에는 어떤 이미지를 넣을지 같이 설계한다
- 최종 산출물에 항상 `References` 를 붙인다

추가로 최근 작성자의 블로그 패턴을 반영해 아래 두 가지도 함께 지킨다.

- 기술 글의 기본 서술은 `합니다체` 로 정제한다
- 제목은 구체 기술명과 글 범위가 보이도록 쓴다

길이에 대해서는 아래 원칙을 따른다.

- 핵심 먼저 쓰되, 설명이 필요한 구간은 충분히 자세히 쓴다
- 짧게 보이기 위한 압축은 피한다
- 특히 경계가 헷갈리기 쉬운 구조, 대안 비교, 실패 처리, 운영 영향은 생략하지 않는다

## 먼저 고정해야 하는 현재 사실

아래 계약은 어떤 글이든 바뀌지 않는 기준이다.

- 공개 발급 런타임은 `Redis reserve -> Kafka publish -> worker consume -> distributed lock -> DB persist`
- `POST /coupon-issues`의 `SUCCESS` 는 `Redis reserve + Kafka broker ack` 성공이지 최종 DB 발급 완료가 아니다
- outbox worker 는 intake durability 가 아니라 `COUPON_ISSUED / COUPON_USED / COUPON_CANCELED` 후속 projection durability 를 담당한다
- Redis 책임은 분리되어 있다
  - issue-state reserve/release/rebuild: Lua
  - distributed lock, processing limit: Redisson
  - 일반 cache: injected `Cache`
- 현재 구조의 대표적인 예외는 worker 의 명시적 component scan rule 이다
- `request table + relay + CDC` 는 현재 저장소에 없다

위 사실과 충돌하는 문장은 reviewer 단계에서 반드시 수정한다.

## 자동 Fan-Out 규칙

사용자가 `블로그 글 써줘`, `기술 글 정리해줘`, `왜 이 구조를 썼는지 글로 써줘`처럼 실제 글 작성을 요청하면
아래 세 agent 를 순서대로 자동 fan-out 한다.

1. `technical-writing-planner`
2. `technical-writing-drafter`
3. `technical-writing-reviewer`

각 agent 책임은 아래와 같다.

- planner
  - 주제를 현재 저장소 사실에 맞게 재정의
  - 로컬 근거와 외부 레퍼런스를 수집
  - 벤치마크 블로그 패턴과 이미지 슬롯을 설계
- drafter
  - 개요와 reference pack 으로 초안 작성
  - 본문 내 `이미지 제안:` 블록과 하단 `References` 섹션 포함
- reviewer
  - 사실 검증, reference 품질, 이미지 적절성, 글 흐름을 함께 점검

단순 주제 추천만 요청받으면 planner 만으로 끝낼 수 있다.

## 권장 읽기 순서

### 현재 구조 이해

1. [../architecture/current-architecture-overview.md](../architecture/current-architecture-overview.md)
2. [../architecture/coupon-issuance-runtime.md](../architecture/coupon-issuance-runtime.md)
3. [../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md](../../coupon/coupon-worker/docs/coupon-kafka-runtime-guide.md)
4. [../../coupon/coupon-worker/docs/phase-2-outbox-worker-runtime.md](../../coupon/coupon-worker/docs/phase-2-outbox-worker-runtime.md)
5. [../architecture/redis-coordination-choice.md](../architecture/redis-coordination-choice.md)
6. [../architecture/coupon-system-expansion-todo.md](../architecture/coupon-system-expansion-todo.md)

### 글쓰기 참조

1. [../../.codex/skills/coupon-technical-writing/references/source-policy.md](../../.codex/skills/coupon-technical-writing/references/source-policy.md)
2. [../../.codex/skills/coupon-technical-writing/references/decision-reference-map.md](../../.codex/skills/coupon-technical-writing/references/decision-reference-map.md)
3. [../../.codex/skills/coupon-technical-writing/references/blog-pattern-benchmarks.md](../../.codex/skills/coupon-technical-writing/references/blog-pattern-benchmarks.md)
4. [../../.codex/skills/coupon-technical-writing/references/visual-playbook.md](../../.codex/skills/coupon-technical-writing/references/visual-playbook.md)
5. [../../.codex/skills/coupon-technical-writing/references/author-style-profile.md](../../.codex/skills/coupon-technical-writing/references/author-style-profile.md)
6. [../../.codex/skills/coupon-technical-writing/references/review-checklist.md](../../.codex/skills/coupon-technical-writing/references/review-checklist.md)
7. [../../.codex/skills/coupon-technical-writing/references/article-template.md](../../.codex/skills/coupon-technical-writing/references/article-template.md)

## Source Policy 요약

- 근거 우선순위
  - 1차: repo 문서와 코드 anchor
  - 2차: 공식 문서와 벤더 문서
  - 3차: 패턴 원저자나 권위 있는 아티클
  - 4차: 작성 스타일 벤치마크용 테크 블로그
- 기본 reference bundle
  - 로컬 근거 1개 이상
  - 외부 factual reference 2개 이상
  - 필요 시 pattern authority 1개
- benchmark 블로그는 `사실의 근거` 가 아니라 `글 구조 참고` 로만 쓴다
- 본문 하단에는 항상 `References` 를 붙인다

## 출력 계약

블로그 초안 요청의 기본 결과는 항상 아래 네 섹션을 포함한다.

1. `개요`
2. `초안`
3. `이미지 제안`
4. `References`

`References` 는 최소 아래 두 카테고리로 나눈다.

- `Local References`
- `External References`

실제로 스타일을 참고한 경우에만 아래 카테고리를 추가한다.

- `Writing Benchmarks`

## 최근 작성자 글에서 반영할 패턴

기술 글 기준으로는 아래 최근 글을 우선 참고한다.

- `2025-03-16` 소프트웨어 디자인도 전략적으로, 전략 패턴에 대해
- `2025-03-02` LocalStack과 DynamoDB 사용기
- `2025-01-01` Java 21, Spring boot 3.4.x 마이그레이션 과정

회고 글은 문장 온도와 친근한 연결 표현만 약하게 참고한다.

- `2025-12-29` 2025년은
- `2025-01-19` 2024년 회고 1편
- `2025-01-28` 2024년 회고 2편

이 분석에서 가져갈 핵심은 아래다.

- 제목은 구체 기술명과 설명 범위를 드러낸다
  - `LocalStack과 DynamoDB 사용기`
  - `Java 21, Spring boot 3.4.x 마이그레이션 과정`
  - `소프트웨어 디자인도 전략적으로, 전략 패턴에 대해`
- 도입은 `왜 이 글을 쓰는지`, `어떤 문제를 겪었는지` 를 먼저 꺼낸다
- 본문은 `계기/서론 -> 구조/과정 -> 예상치 못한 이슈 -> 후기` 흐름을 자주 쓴다
- 문장 종결은 기본적으로 `합니다`, `했습니다`, `해보겠습니다` 계열이다
- 아주 약한 구어체나 비유는 허용하지만, 본문 중심은 정제된 기술 설명으로 회수한다

## 이미지 사용 규칙

- 이미지가 없어도 이해되는 글이면 억지로 넣지 않는다
- 다만 아래 구간은 이미지 우선 검토 대상이다
  - 비동기 요청 흐름 설명
  - 상태 전이와 보상 규칙 설명
  - 대안 비교 설명
  - 운영 지표와 병목 설명
- 기본적으로 글당 1~2개의 고가치 이미지면 충분하다
- 자세한 선택 기준은 [visual-playbook.md](../../.codex/skills/coupon-technical-writing/references/visual-playbook.md)를 따른다

## 벤치마크 패턴 적용 규칙

- 올리브영 패턴
  - 강한 부제
  - 목차 선제시
  - 사례별 비교표
  - `최종 결정` 과 `교훈` 정리
- 무신사 패턴
  - 질문형 도입
  - 대안과 시행착오 공개
  - 그림 번호와 다이어그램 활용
  - 정량 효과 제시
- velog 30+ 우선, 확인이 어려우면 트렌딩/추천 대체
  - 짧은 문단
  - 질문형 소제목
  - 초반에 결과 또는 결론 힌트 제시

최종 글은 위 패턴을 그대로 복제하지 말고 아래 하이브리드 규칙으로 합친다.

- 도입: 질문형 문제 제기
- 중간: 비교표 또는 기준표
- 핵심: 다이어그램 1~2개
- 후반: 운영/실패/한계
- 마무리: 교훈 2~3개 + `References`

단, `짧은 문단` 규칙은 `설명을 덜 하라` 는 뜻이 아니다.

- 문단은 짧게 쪼개더라도 필요한 설명량은 충분히 확보한다
- 오해 여지가 큰 구간은 예시, 비교, 반례, 실패 시나리오를 더 붙인다
- reader 가 `그래서 왜 그런가` 를 물을 만한 지점은 한 단계 더 설명한다

## 개인 문체 적용 규칙

작성자는 [ybchar.dev/tech-blog](https://ybchar.dev/tech-blog) 와 [velog](https://velog.io/@uiurihappy) 를 운영하므로,
기본 글 구조 위에 아래 톤을 약하게 섞는다.

- 첫 문단은 너무 딱딱한 교과서 톤보다 `문제 공감` 이 먼저 오게 쓴다
- 설명은 실무 관점과 pain point 해결 중심으로 잡는다
- 지나치게 권위적인 단정 대신 `내가 이 문제를 이렇게 이해했다` 는 1인칭 관찰 톤을 허용한다
- 본문 기본 종결은 `합니다체` 로 정리한다
- `한다체` 는 직접 인용, 코드/표 설명, 요약 메모가 아니면 지양한다
- 비유는 글당 1~2개 정도만 사용한다
- 비유는 기술 의미를 더 잘 이해시키는 범위에서만 쓴다
- 과한 개그, 밈, 감탄사 남발은 피한다
- 기술 사실 설명이 끝난 뒤 가볍게 풀어주는 식의 비유가 가장 안전하다

헤드라인은 아래 우선순위로 잡는다.

1. 구체 기술명 + 사용기/과정
2. 짧은 훅 + 쉼표 + 주제 + `에 대해`
3. 질문형 제목

예시:

- `Redis Lua로 쿠폰 재고를 선점한 과정`
- `쿠폰 발급도 접수와 처리를 분리해야 하는 이유, Kafka intake 설계에 대해`
- `왜 coupon-issues SUCCESS는 발급 완료가 아닐까`

예시 방향:

- outbox 는 `메인 업무가 끝난 뒤 영수증을 별도 보관함에 넣어 후속 처리를 맡기는 구조`
- Lua reserve 는 `입구에서 번호표와 좌석 수를 동시에 확인하는 안내 데스크`
- Kafka intake 분리는 `주문 접수와 실제 조리를 같은 테이블에서 처리하지 않는 구조`

## 금지 규칙

- `SUCCESS` 를 최종 발급 완료처럼 쓰지 않는다
- outbox 를 intake durability 용도로 설명하지 않는다
- `request table`, `relay`, `CDC`, durable acceptance 를 이미 구현된 것처럼 쓰지 않는다
- benchmark 블로그를 기술 사실의 근거처럼 쓰지 않는다
- 개인 문체를 살리겠다고 기술 설명보다 비유가 앞서지 않게 한다
- `한다체` 와 과한 교과서 톤으로 끝까지 밀지 않는다
- 예전 phase 문서를 현재 동작의 단일 근거처럼 쓰지 않는다
- worker component scan 예외를 숨기지 않는다
