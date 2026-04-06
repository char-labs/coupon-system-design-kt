---
name: k6-scenario-builder
description: "k6 부하테스트 시나리오를 작성하고 RUNBOOK.md를 업데이트하는 스킬. 10k RPS 고처리량 시나리오, MQ 비동기 발급 검증 시나리오, 런북 추가를 수행한다. 'k6 시나리오 추가', '부하테스트 시나리오 작성', '10k RPS 테스트', 'MQ 부하테스트', '런북 업데이트' 등의 요청 시 반드시 이 스킬을 사용할 것."
---

# k6 Scenario Builder

기존 k6 시나리오 구조를 유지하면서 고처리량/MQ 시나리오를 추가하고 런북을 업데이트한다.

## 작성 절차

### Step 1: 기존 구조 파악

다음 파일들을 읽어 기존 패턴을 파악한다:
- `load-test/k6/baseline.js` — 시나리오 구조 참조
- `load-test/k6/lib/config.js` — 설정 패턴
- `load-test/k6/lib/api.js` — API 헬퍼 패턴
- `load-test/k6/lib/coupon.js` — 쿠폰 API 헬퍼

### Step 2: 10k RPS 고처리량 시나리오

`load-test/k6/high-throughput.js`를 작성한다:

```javascript
import { check } from 'k6';
import http from 'k6/http';
import { config } from './lib/config.js';
import { issueCoupon } from './lib/coupon.js';

export const options = {
  scenarios: {
    high_throughput: {
      executor: 'ramping-arrival-rate', // RPS 기반 (VU 기반 아님)
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 500,
      maxVUs: 2000,
      stages: [
        { target: 1000, duration: '2m' },   // 워밍업
        { target: 5000, duration: '3m' },   // 중간 부하
        { target: 10000, duration: '5m' },  // 목표 부하
        { target: 10000, duration: '5m' },  // 지속 검증
        { target: 0, duration: '1m' },      // 종료
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p95<500', 'p99<1000'],  // 95th < 500ms
    http_req_failed: ['rate<0.01'],               // 오류율 < 1%
    checks: ['rate>0.99'],
  },
};
```

**핵심: `ramping-arrival-rate` executor**
- VU 수가 아닌 초당 요청 수를 직접 지정
- 서버 응답이 느려져도 지정된 RPS를 유지하려 시도
- 10k RPS 달성 불가 시 `dropped_iterations` 지표로 확인

### Step 3: MQ 비동기 시나리오

`load-test/k6/mq-async.js`를 작성한다. 비동기 발급은 202 Accepted를 받고 결과를 폴링한다:

```javascript
export const options = {
  scenarios: {
    mq_async_issue: {
      executor: 'constant-vus',
      vus: config.mqAsyncVus || 200,
      duration: config.mqAsyncDuration || '5m',
    },
  },
  thresholds: {
    // 비동기는 완료까지 걸리는 전체 시간으로 측정
    'coupon_issue_total_ms': ['p95<3000'], // 3초 내 완료
    http_req_failed: ['rate<0.01'],
  },
};

export default function(data) {
  // 1. 발급 요청 (202 Accepted 기대)
  const issueRes = issueCouponAsync(token, couponId);
  check(issueRes, { 'issue accepted': r => r.status === 202 });

  // 2. 결과 폴링 (최대 10초)
  const issueId = issueRes.json('id');
  const result = pollIssueResult(token, issueId, { maxWait: 10000, interval: 200 });
  check(result, { 'issue completed': r => r.status === 'COMPLETED' });
}
```

### Step 4: RUNBOOK.md 업데이트

`load-test/k6/RUNBOOK.md`에 새 섹션을 추가한다:

```markdown
### High Throughput (10k RPS 목표)

ramping-arrival-rate 방식으로 10,000 RPS까지 점진적으로 부하를 높입니다.

\`\`\`bash
node load-test/k6/run-with-slack.mjs high-throughput --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ADMIN_EMAIL=loadtest-admin@coupon.local \
  -e ADMIN_PASSWORD='admin1234!'
\`\`\`

**정상 신호:**
- `dropped_iterations = 0`: 목표 RPS 달성
- `p95 < 500ms`: 95th percentile 지연 기준 통과
- `p99 < 1000ms`: 99th percentile 지연 기준 통과

**이상 신호:**
- `dropped_iterations` 증가: 서버가 목표 RPS를 소화 못 함
- `p95` 계단식 상승: 특정 부하 구간에서 포화
- Tomcat thread 풀 포화 → `/actuator/prometheus`에서 확인

### MQ 비동기 발급

\`\`\`bash
node load-test/k6/run-with-slack.mjs mq-async --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e MQ_ASYNC_VUS=200
\`\`\`
```

### Step 5: config.js 확장

새 시나리오의 환경변수를 `lib/config.js`에 추가한다:

```javascript
export const config = {
  // 기존 설정 유지
  ...existingConfig,

  // 신규
  highThroughputMaxVus: parseInt(__ENV.HIGH_THROUGHPUT_MAX_VUS || '2000'),
  mqAsyncVus: parseInt(__ENV.MQ_ASYNC_VUS || '200'),
  mqAsyncDuration: __ENV.MQ_ASYNC_DURATION || '5m',
};
```

## 출력

- `load-test/k6/high-throughput.js`
- `load-test/k6/mq-async.js` (MQ 도입 시)
- 업데이트된 `load-test/k6/RUNBOOK.md`
- `_workspace/03_loadtest_summary.md`

## 참조

`references/k6-executors.md` — executor 선택 가이드
