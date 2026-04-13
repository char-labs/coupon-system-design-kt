# Visual Playbook

## 목적

이 문서는 블로그 글로만 설명하기 어려운 지점에 어떤 시각 자료를 넣으면 좋은지 정리한다.

## 기본 원칙

- 글당 1~2개의 고가치 이미지면 충분하다
- 이미지는 `설명 압축` 용도로 쓴다
- 같은 내용을 그림과 글로 중복해서 길게 반복하지 않는다
- 이미지가 없다면 이해가 어렵거나 비교가 느린 구간에만 넣는다
- Mermaid 노드와 화살표는 기본적으로 독자 친화적인 한국어 의미 라벨로 쓴다
- raw 명령명이나 구현 세부는 코드 블록이나 본문에서 설명하고, 다이어그램은 `의미 전달` 을 우선한다
- 명령명이 꼭 필요할 때만 괄호나 캡션 보조 설명으로 제한적으로 붙인다

## 이미지 유형별 사용 기준

| 유형 | 언제 쓰는가 | 이 저장소 예시 |
| --- | --- | --- |
| sequence diagram | 요청 흐름, 비동기 경계, 보상 흐름을 설명할 때 | `POST /coupon-issues` intake -> Kafka -> worker |
| architecture diagram | 컴포넌트 책임과 배치를 한 장에 보여줄 때 | API / Redis / Kafka / worker / DB / outbox |
| comparison table | 대안 비교나 책임 분리를 한 번에 보여줄 때 | Lua vs Redisson Lock vs RRateLimiter |
| flowchart | 상태 전이, retry, DLQ, release 조건을 설명할 때 | outbox 상태, reserve release 보상 규칙 |
| metric graph | 운영 지표와 병목을 설명할 때 | `worker.limit`, lag, write latency 조합 |
| before/after diagram | 현재와 대안 또는 현재와 미래를 비교할 때 | direct sync write vs async intake / future durable acceptance |

## 토픽별 기본 추천

### Outbox

- 1순위: flowchart
- 2순위: sequence diagram
- 추천 구간:
  - `왜 intake 가 아니라 projection 인가`
  - `t_outbox_event -> poll -> dispatch -> activity`

### Redis Lua

- 1순위: sequence diagram
- 2순위: comparison table
- 추천 구간:
  - reserve 시 duplicate / sold out / success 분기
  - Lua 와 lock 의 책임 분리

### Kafka

- 1순위: sequence diagram
- 2순위: architecture diagram
- 추천 구간:
  - `202 Accepted` 의미
  - intake 와 worker execution 경계

### Lock / Limiter

- 1순위: comparison table
- 2순위: flowchart
- 추천 구간:
  - Redisson lock 과 RRateLimiter 의 차이
  - acquire / execute / release 흐름

### Observability

- 1순위: metric graph
- 2순위: flowchart
- 추천 구간:
  - `requestId`, `acceptedAt`, `phase` 가 이어지는 위치
  - `lag + limiter + DLQ` 를 같이 보는 이유

## 본문 내 표시 형식

아래 형식으로 직접 넣는다.

```md
이미지 제안:
- 구간: `왜 Kafka 로 intake 와 execution 을 분리했는가`
- 유형: `sequence diagram`
- 이유: `API acceptance 와 worker persist 사이의 경계를 글보다 빠르게 보여준다`
- 캡션 초안: `그림 1. coupon issue intake 와 worker final persist 흐름`
```

## Mermaid 라벨링 규칙

- 우선순위는 `독자가 한눈에 이해하는 의미` 다
- `EXPIRE occupied-count` 보다는 `점유 수 상태 만료 시간 갱신`
- `SADD reserved-users userId` 보다는 `사용자 선점 기록 추가`
- `SISMEMBER usersKey userId == 1?` 보다는 `이미 선점한 사용자일까?`
- participant 이름도 가능하면 `API 서버`, `워커`, `처리량 제한기` 처럼 의미를 드러낸다
- raw 명령명은 코드 블록, 표, 또는 다이어그램 아래 설명 문장에서 회수한다

## 과잉 사용 신호

- 모든 섹션에 이미지를 넣으려 한다
- 비교표와 글이 완전히 같은 내용을 반복한다
- 본문 논리 없이 그림만 늘어난다
- 그림이 없어도 같은 속도로 이해되는 구간에 억지로 넣는다
