# Review Checklist

## 목적

이 문서는 `technical-writing-reviewer` 가 확인해야 하는 항목을 정리한다.

## 1. 사실 검증

- `POST /coupon-issues` 의 `SUCCESS` 를 최종 DB 발급 완료처럼 썼는가
- outbox 를 intake durability 로 설명했는가
- `request table`, `relay`, `CDC`, durable acceptance 를 이미 구현된 것처럼 썼는가
- worker component scan 예외를 숨겼는가
- Redis Lua, Redisson lock, RRateLimiter 책임이 섞였는가
- Kafka, Redis, DB 중 최종 truth 의 위치를 잘못 설명했는가

## 2. Reference 품질

- `References` 섹션이 있는가
- `Local References` 와 `External References` 가 분리되어 있는가
- 외부 근거가 최소 2개 이상 있는가
- 공식 문서보다 2차 블로그에 과도하게 기대지 않았는가
- benchmark 블로그가 구조 참고로만 쓰였는가
- 본문의 핵심 주장과 reference 가 실제로 연결되는가

## 3. 이미지 제안

- 이미지 제안이 있는가
- 글로만 충분한 구간에 이미지를 과하게 넣지 않았는가
- 비동기 경계, 대안 비교, 상태 전이 같은 구간에 필요한 이미지가 빠지지 않았는가
- 이미지 유형과 캡션이 섹션 논리에 맞는가

## 4. 글 구조

- 질문형 도입이 있는가
- 대안과 현재 선택이 모두 보이는가
- `왜 이 선택이 맞는가` 와 `한계` 가 함께 있는가
- 문단이 너무 길지 않은가
- 목차나 소제목이 읽기 흐름을 돕는가
- 마무리에 교훈 또는 다음 단계가 있는가

## 5. Reviewer 출력 형식

Reviewer 는 가능하면 아래 형식으로 반환한다.

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
