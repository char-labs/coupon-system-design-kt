# k6 Executor 선택 가이드

## 주요 Executor 비교

| Executor | 제어 대상 | 적합한 용도 |
|----------|----------|-----------|
| `constant-vus` | VU 수 고정 | 동시 사용자 수 기준 부하 |
| `ramping-vus` | VU 수 증감 | 단계적 사용자 증가 |
| `constant-arrival-rate` | RPS 고정 | 정확한 RPS 측정 |
| `ramping-arrival-rate` | RPS 증감 | 처리량 한계 탐색 |
| `per-vu-iterations` | VU당 반복 수 | 정확한 반복 횟수 제어 |
| `shared-iterations` | 전체 반복 수 | 총 요청 수 제한 |

## 고처리량 테스트에는 `ramping-arrival-rate`

VU 기반 executor의 문제점:
- 서버가 느려지면 VU가 대기하면서 실제 RPS가 줄어듦
- "서버가 얼마나 버티는가"보다 "서버가 얼마나 느려지는가"만 측정

`ramping-arrival-rate`의 장점:
- 목표 RPS를 유지하려 시도
- 서버가 포화되면 `dropped_iterations`로 정확히 측정
- 실제 운영 부하 패턴을 더 잘 시뮬레이션

## `dropped_iterations` 해석

- `dropped_iterations = 0`: 서버가 목표 RPS를 모두 처리
- `dropped_iterations > 0`: 서버가 일부 요청을 처리 못 함 = 한계 초과
- `dropped_iterations / total_iterations` = 처리 실패율

## preAllocatedVUs 설정

```
preAllocatedVUs = 목표RPS × (예상평균응답시간ms / 1000)
               = 10,000 × 0.05 = 500
```

`maxVUs`는 응답시간이 길어질 때를 대비한 버퍼. 보통 preAllocatedVUs × 4.
