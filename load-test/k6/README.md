# k6 Load Test Guide

이 디렉터리는 `springboot-coupon-system`과 유사한 `Redis reserve -> Kafka -> consumer issue` 계약을 검증하는 `k6` 시나리오를 담고 있습니다.

현재 공개 발급 계약은 아래 경로가 기준입니다.

- `POST /coupon-issues`

핵심 원칙은 단순합니다.

- 발급 API는 즉시 `SUCCESS`, `DUPLICATE`, `SOLD_OUT` 중 하나를 반환합니다.
- `SUCCESS`는 Kafka 전송까지 끝난 발급 요청 성공 의미입니다.
- HTTP status는 `SUCCESS=202`, `DUPLICATE|SOLD_OUT=200` 입니다.
- load-test 시나리오는 실제 `/signup`, `/signin`, `/coupon-issues`를 사용합니다.

상세 운영 절차는 [RUNBOOK.md](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/RUNBOOK.md)를 봅니다.

## 공식 시나리오

- `smoke.js`
  - 실제 회원가입/로그인 세션으로 `POST /coupon-issues`를 호출하고, 내 쿠폰 조회를 통해 발급 완료를 확인한 뒤 쿠폰 사용까지 검증합니다.
- `baseline.js`
  - 일반 사용량 기준으로 발급, 사용, 취소, 내 쿠폰 조회를 섞어서 호출합니다.
- `issue-burst.js`
  - 사용자 다수가 같은 쿠폰에 동시에 발급을 요청합니다.
  - 즉시 응답 분포와 최종 발급 건수, 잔여 재고, 정합성을 함께 검증합니다.
- `contention.js`
  - 실제 회원가입으로 세션을 미리 준비한 뒤 같은 쿠폰으로 경합 상황을 재현합니다.
- `issue-overload.js`
  - 일정 시간 동안 즉시 SUCCESS 응답을 반복해서 sustained load 안정성을 확인합니다.
- `issue-ramp.js`
  - 실제 회원가입으로 준비한 세션 기준 발급 intake 성능만 확인합니다.
  - immediate SUCCESS 결과만 측정합니다.
- `issue-real-ramp.js`
  - 실제 인증 토큰으로 `/coupon-issues`를 호출해 immediate issue 성능을 확인합니다.
  - Kafka enqueue까지의 발급 진입 성능만 측정합니다.

## 빠른 시작

스택을 올립니다.

```bash
docker compose \
  -f docker/docker-compose.yml \
  -f docker/docker-compose.k6-observability.yml \
  up --build
```

준비를 확인합니다.

```bash
curl http://127.0.0.1:18080/ping
curl http://127.0.0.1:18080/actuator/health
curl -X POST http://127.0.0.1:18080/signin \
  -H 'Content-Type: application/json' \
  -d '{"email":"loadtest-admin@coupon.local","password":"admin1234!"}'
```

추천 실행 순서는 아래입니다.

```bash
./load-test/k6/run-local-kafka-runbook.sh up
./load-test/k6/run-local-kafka-runbook.sh check
./load-test/k6/run-local-kafka-runbook.sh smoke
./load-test/k6/run-local-kafka-runbook.sh burst
```

쿠폰 1개에 대해 재고만 바꿔 보고 싶으면 아래 런북을 사용합니다.

```bash
./load-test/k6/run-single-coupon-stock-runbook.sh up
./load-test/k6/run-single-coupon-stock-runbook.sh check
./load-test/k6/run-single-coupon-stock-runbook.sh exact
```

## 대표 실행 예시

### Smoke

```bash
node load-test/k6/run-with-slack.mjs smoke --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080
```

### Burst

```bash
node load-test/k6/run-with-slack.mjs issue-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_BURST_VUS=1000 \
  -e ISSUE_BURST_STOCK=1000 \
  -e ISSUE_POLL_TIMEOUT_SECONDS=30 \
  -e ISSUE_POLL_INTERVAL_MS=500
```

### Overload

```bash
node load-test/k6/run-with-slack.mjs issue-overload --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_OVERLOAD_VUS=100 \
  -e ISSUE_OVERLOAD_DURATION=10m
```

### Ramp

```bash
node load-test/k6/run-with-slack.mjs issue-ramp --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_RAMP_STAGE1_TARGET=3000 \
  -e ISSUE_RAMP_STAGE3_TARGET=5000 \
  -e ISSUE_RAMP_STAGE5_TARGET=7000
```

## 주요 환경변수

- `BASE_URL`
  - 기본값은 `http://127.0.0.1:18080` 입니다.
- `ISSUE_POLL_TIMEOUT_SECONDS`
  - 내 쿠폰 조회 기반 완료 대기에서 사용하는 최대 시간입니다.
- `ISSUE_POLL_INTERVAL_MS`
  - 완료 대기 polling 간격입니다.
- `ISSUE_BURST_VUS`
  - burst 동시 사용자 수입니다.
- `ISSUE_BURST_STOCK`
  - burst 검증용 쿠폰 재고입니다.
- `ISSUE_OVERLOAD_VUS`
  - overload 동안 유지할 동시 사용자 수입니다.
- `ISSUE_OVERLOAD_DURATION`
  - overload 실행 시간입니다.
- `ISSUE_RAMP_STAGE*`
  - ramp 단계별 duration/target입니다.
- `ISSUE_REAL_RAMP_USER_POOL_SIZE`
  - real-ramp에서 미리 준비할 실제 사용자 세션 수입니다.

## Slack 보고

[run-with-slack.mjs](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/run-with-slack.mjs)는 [load-test/k6/.env](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/.env)를 자동으로 읽고 Slack webhook으로 결과를 보낼 수 있습니다.

예시:

```dotenv
LOAD_TEST_SLACK_WEBHOOK=
LOAD_TEST_PROFILE=local
LOAD_TEST_SLACK_NOTIFY_ON=always
```

webhook이 비어 있으면 전송 대신 `load-test/k6/results/` 아래 preview 파일만 남깁니다.

## 해석 포인트

- `issue-burst`
  - `success + out_of_stock` 분포
  - `final issued count`
  - `final remaining quantity`
  - `integrity ok rate`
  - `expected result ok rate`
- `issue-overload`
  - `success count`
  - `sold_out count`
  - `unexpected failure count`
- `issue-ramp`, `issue-real-ramp`
  - success count
  - p95 latency
  - http error rate

정합성 기준에서 가장 중요한 값은 `issued + remaining == initial stock` 입니다.
