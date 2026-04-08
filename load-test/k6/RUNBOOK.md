# k6 Dashboard Runbook

이 문서는 `springboot-coupon-system`과 유사한 `Redis reserve -> Kafka -> consumer issue` 쿠폰 발급 계약 기준으로 `k6 -> InfluxDB -> Grafana` 실행 절차를 정리합니다.
런타임 계약과 로그 기반 관측 규칙은 [docs/architecture/coupon-issuance-runtime.md](/Users/yunbeom/ybcha/coupon-system-design-kt/docs/architecture/coupon-issuance-runtime.md)를 기준으로 봅니다.

현재 발급의 기준 공개 계약은 아래입니다.

- `POST /coupon-issues`
- `POST /restaurant-coupons/issue`
- `GET /coupon-issues/my`
- `GET /coupon-issues/coupons/{couponId}`

`POST /coupon-issues` 는 raw text가 아니라 `ApiResponse` envelope를 반환합니다. 측정 기준은 `data.result` 이고 성공 시 HTTP `202`, 중복/품절 시 HTTP `200` 입니다.

## 추천 흐름

가장 빠른 진입점은 아래 두 개입니다.

```bash
./load-test/k6/run-local-kafka-runbook.sh full
./load-test/k6/run-single-coupon-stock-runbook.sh full
```

`full` 순서는 아래입니다.

- local-kafka runbook: `up -> check -> smoke -> issue-burst`
- single-coupon runbook: `up -> check -> exact -> oversubscribed -> single-stock`

## 1. 스택 올리기

```bash
docker compose \
  -f docker/docker-compose.yml \
  -f docker/docker-compose.k6-observability.yml \
  up --build
```

준비 확인:

```bash
curl http://127.0.0.1:18080/ping
curl http://127.0.0.1:18080/actuator/health
curl -X POST http://127.0.0.1:18080/signin \
  -H 'Content-Type: application/json' \
  -d '{"email":"loadtest-admin@coupon.local","password":"admin1234!"}'
curl -i http://localhost:8086/ping
curl http://localhost:3000/api/health
```

기본 포트:

- app: `http://127.0.0.1:18080`
- worker actuator: `http://127.0.0.1:18081/actuator/health`
- InfluxDB: `http://localhost:8086`
- Grafana: `http://localhost:3000`
- Loki: `http://localhost:3100`
- Alloy: `http://localhost:12345`
- Kafka UI: `http://localhost:18085`

## 2. Grafana 열기

1. `http://localhost:3000`
2. `admin / admin`
3. `Dashboards`
4. `k6 Overview`
5. `coupon-runtime -> Coupon Issuance Runtime`
6. ad-hoc 탐색은 `Explore -> Loki` 에서 확인

권장 설정:

- time range: `Last 15 minutes`
- refresh: `5s` 또는 `10s`

로그 예시:

```logql
{service_name="coupon-app"} | json | message =~ ".*event=coupon.issue.*phase=intake.publish.*"
```

```logql
{service_name="coupon-worker"} | json | message =~ ".*event=coupon.issue.*phase=worker.dlq.*"
```

`Coupon Issuance Runtime` 대시보드는 아래 패널을 기본 제공합니다.

- `Accepted Requests`, `Publish Failures`, `Worker Success`, `DLQ Count`
- `Immediate Result Breakdown`, `Publish Outcome`, `Worker Outcome`
- `Worker Processing Limit`, `Publish Duration p95`, `Accepted To Persist p95`
- `Coupon Issue Logs`, `Failures And Retries`

특정 발급 요청만 추적할 때는 `RequestId regex` 변수에 request id를 넣으면 됩니다.

## 3. 대표 실행

### Smoke

```bash
node load-test/k6/run-with-slack.mjs smoke --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080
```

목적:

- 관리자 로그인
- 쿠폰 생성/활성화
- 사용자 회원가입/로그인
- immediate issue result (`data.result`)
- 내 쿠폰 조회 기반 발급 완료 확인
- coupon use

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

### Restaurant Burst

```bash
node load-test/k6/run-with-slack.mjs restaurant-issue-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_BURST_VUS=1000 \
  -e ISSUE_BURST_STOCK=1000
```

목적:

- 동일 restaurantId 동시 발급
- immediate `SUCCESS / SOLD_OUT / DUPLICATE` 분포 확인
- 최종 발급 건수 / 잔여 재고 검증
- `issued + remaining == initial stock` 확인

목적:

- 동일 쿠폰 동시 발급
- immediate `SUCCESS / SOLD_OUT / DUPLICATE` 분포 확인
- 최종 발급 건수 / 잔여 재고 검증
- `issued + remaining == initial stock` 확인

oversubscription 확인 예시:

```bash
node load-test/k6/run-with-slack.mjs issue-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_BURST_VUS=1000 \
  -e ISSUE_BURST_STOCK=900
```

### Overload

```bash
node load-test/k6/run-with-slack.mjs issue-overload --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_OVERLOAD_VUS=100 \
  -e ISSUE_OVERLOAD_DURATION=10m
```

목적:

- immediate SUCCESS request 반복
- Kafka enqueue까지의 즉시 응답 안정성 확인
- unexpected failure 유무 확인

### Ramp

```bash
node load-test/k6/run-with-slack.mjs issue-ramp --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_RAMP_STAGE1_TARGET=3000 \
  -e ISSUE_RAMP_STAGE3_TARGET=5000 \
  -e ISSUE_RAMP_STAGE5_TARGET=7000
```

목적:

- 실제 회원가입으로 준비한 세션 기준 immediate issue throughput 확인
- terminal status polling 없이 issue latency만 측정

### Real Ramp

```bash
node load-test/k6/run-with-slack.mjs issue-real-ramp --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_REAL_RAMP_USER_POOL_SIZE=7000 \
  -e ISSUE_REAL_RAMP_COUPON_POOL_SIZE=1000
```

목적:

- 실제 인증 토큰 기준 immediate issue throughput 확인
- synthetic shortcut 없이 `/coupon-issues` 직접 측정

## 4. setup 전략

burst, overload, ramp 계열 시나리오는 측정 구간 전에 실제 API로 사용자 세션과 쿠폰을 준비합니다.

- `/signup`
- `/signin`
- `/coupons`

의도는 단순합니다.

- 발급 측정 구간에서는 실제 비즈니스 API만 호출
- setup 단계에서만 실제 회원가입/로그인을 먼저 수행
- 측정값은 즉시 발급 응답과 최종 재고 정합성에 집중

## 5. 결과 해석

### 반드시 보는 값

- `http_req_failed`
- `checks`
- `http_req_duration p95`
- `http_req_duration p99`

### Burst에서 추가로 보는 값

- `issue_burst_success_count`
- `issue_burst_out_of_stock_count`
- `issue_burst_final_issued_count`
- `issue_burst_final_remaining_quantity`
- `issue_burst_integrity_ok`
- `issue_burst_expected_result_ok`

해석 기준:

- `success_count == min(vus, stock)` 이면 기대에 맞습니다.
- `final_issued + final_remaining == initial_stock` 이면 재고 정합성은 맞습니다.
- oversubscribed case에서 `out_of_stock > 0` 은 정상일 수 있습니다.

### Overload에서 추가로 보는 값

- `issue_overload_success_count`
- `issue_overload_sold_out_count`
- `issue_overload_unexpected_failure_count`

해석 기준:

- `unexpected_failure_count == 0`

### Ramp에서 추가로 보는 값

- `issue_ramp_success_count`
- `issue_real_ramp_success_count`

해석 기준:

- immediate issue throughput 증가 시 p95가 급격히 꺾이는 지점 확인
- error rate 상승 시작 지점 확인

## 6. 자주 보는 실패 패턴

### app 준비 전 실행

증상:

- `admin_signin_ready`
- `app_ready`

조치:

- `/actuator/health`
- `/signin`

### capacity exhausted

증상:

- 실제 사용자 세션 풀 또는 coupon pool 부족

조치:

- `ISSUE_OVERLOAD_USER_POOL_SIZE`
- `ISSUE_OVERLOAD_COUPON_POOL_SIZE`
- `ISSUE_RAMP_USER_POOL_SIZE`
- `ISSUE_RAMP_COUPON_POOL_SIZE`

### 서버 내부 오류

증상:

- `status=500`
- `CannotCreateTransactionException`

조치:

- app/worker 로그
- DB pool
- Redis lock 대기
- Kafka lag

### burst 정합성 실패

증상:

- `issue_burst_integrity_ok rate != 1`
- `issue_burst_expected_result_ok rate != 1`

조치:

- `/coupon-issues/coupons/{couponId}` totalCount
- remaining quantity
- server error count

## 7. Slack 보고

`run-with-slack.mjs`는 저장소 루트 `.env`를 우선으로 읽고, 없으면 `load-test/k6/.env`를 fallback 으로 읽습니다.

```dotenv
LOAD_TEST_SLACK_WEBHOOK=
LOAD_TEST_PROFILE=local
LOAD_TEST_SLACK_NOTIFY_ON=always
```

지원 값:

- `failure`
- `always`
- `never`

webhook이 비어 있으면 `load-test/k6/results/`에 preview 텍스트만 남깁니다.

권장 시작점은 루트 `[.env.example](/Users/yunbeom/ybcha/coupon-system-design-kt/.env.example)` 입니다.
