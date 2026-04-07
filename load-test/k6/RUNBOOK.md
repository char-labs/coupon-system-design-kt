# k6 Dashboard Runbook

이 문서는 `k6 -> InfluxDB -> Grafana` 흐름으로 테스트를 실행하고, 대시보드를 보면서 결과를 해석하는 최소 운영 가이드입니다.

현재 기본 기준 시나리오는 아래입니다.

- 사용자 `1,000명`
- 쿠폰 `1개`
- 쿠폰 수량 `1,000개`
- 모든 사용자가 같은 시점에 발급 시도
- 검증 목표: `중복 발급 없음`, `최종 발급 건수 1,000`, `최종 잔여 재고 0`, `issued + remaining == initial stock`

## 시나리오 맵

아래 시나리오는 모두 "쿠폰 발급 시스템에 어떤 부하가 들어오는지"를 먼저 가정하고 시작합니다.

- `smoke`
  - 상황: 사용자 1명이 쿠폰 발급 요청 1건을 보냅니다.
  - 목적: 배포 직후 request accepted-model, Kafka relay, consumer, 상태 조회가 최소한 정상 동작하는지 확인합니다.
- `issue-request-smoke`
  - 상황: synthetic 사용자 1명이 비동기 쿠폰 발급 요청 1건을 보냅니다.
  - 목적: load-test 전용 accepted endpoint와 request 상태 수렴이 정상인지 확인합니다.
- `ramp`
  - 상황: synthetic 사용자가 점진적으로 증가합니다.
  - 기본값: `3,000 -> 5,000 -> 7,000 VU`
  - 목적: 인증 비용 없이 Kafka intake 자체가 어디까지 버티는지 봅니다.
- `real-ramp`
  - 상황: 실제 인증된 사용자 세션 풀이 점진적으로 발급 요청을 보냅니다.
  - 기본값: 사용자 풀 `7,000명`, 쿠폰 풀 `1,000개`, 쿠폰당 재고 `100,000개`
  - 목적: 실사용자 가정으로 intake 성능을 확인합니다.
- `burst`
  - 상황: 한 종류의 쿠폰에 사용자가 동시에 몰립니다.
  - 기본값: `사용자 1,000명 동시 발급 시도 / 쿠폰 수량 1,000개`
  - 목적: 동시성 상황에서 정합성, 중복 방지, 재고 수렴이 맞는지 봅니다.
- `overload`
  - 상황: 일정 시간 동안 지속적으로 발급 요청이 유입됩니다.
  - 기본값: `50 VU`, `10분`, 쿠폰 풀 `200개`, 쿠폰당 재고 `100,000개`
  - 목적: sustained load에서 worker, Kafka, request 상태 수렴이 안정적인지 봅니다.

한 번에 따라칠 수 있는 추천 진입점은 아래와 같습니다.

- 최소 회귀 확인
  - `./load-test/k6/run-local-kafka-runbook.sh smoke`
- 동시성 검증
  - `./load-test/k6/run-local-kafka-runbook.sh burst`
- 전체 추천 플로우
  - `./load-test/k6/run-local-kafka-runbook.sh full`

한 번에 따라칠 수 있는 추천 진입점은 [run-local-kafka-runbook.sh](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/run-local-kafka-runbook.sh) 입니다.

쿠폰 1개에 대해 재고 수량만 바꿔가며 정합성을 보고 싶으면 [run-single-coupon-stock-runbook.sh](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/run-single-coupon-stock-runbook.sh)를 사용합니다.

```bash
./load-test/k6/run-local-kafka-runbook.sh full
```

현재 `full`은 `up -> check -> smoke -> burst` 순서입니다. 먼저 기본 회귀와 1,000명 동시 발급 정합성을 확인하고, ramp/overload는 필요할 때만 추가합니다.

쿠폰 1개 재고 검증 전용 추천 진입점은 아래입니다.

```bash
./load-test/k6/run-single-coupon-stock-runbook.sh full
```

세부 제어가 필요하면 아래 단계별 명령을 직접 사용합니다.

## 1. 스택 올리기

기본 앱 스택과 observability overlay를 함께 올립니다.

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
curl -X POST http://127.0.0.1:18080/load-test/admin/signin \
  -H 'Content-Type: application/json' \
  -d '{}'
curl -i http://localhost:8086/ping
curl http://localhost:3000/api/health
```

`k6` 스크립트는 `setup()`에서 `/actuator/health`를 확인한 뒤, local 또는 load-test profile 전용 `/load-test/admin/signin`으로 관리자 계정 보장과 토큰 발급을 먼저 시도합니다. synthetic 시나리오는 `setup()`에서 `/load-test/users/prepare`로 필요한 사용자 수를 먼저 bulk prepare하고, measured phase에서는 쿠폰 발급 요청만 보내도록 맞췄습니다. synthetic 발급 엔드포인트는 전달된 k6 user id를 deterministic load-test user row로 매핑해서 FK 제약을 유지합니다. 그래도 첫 실행 안정성을 위해 위 준비 확인을 먼저 하는 편이 좋습니다.

Kafka accepted-model 시나리오는 아래 `local` 또는 `load-test` profile 전용 synthetic endpoint를 사용합니다.

- `POST /load-test/coupons/{couponId}/issue-requests`
- `GET /load-test/coupon-issue-requests/{requestId}`

이 경로들은 JWT 인증을 우회해서, 회원가입/로그인 비용이 아니라 Kafka relay/consumer 파이프라인의 request intake와 수렴 특성을 보는 데 집중합니다.

`BASE_URL`은 `localhost` 대신 `127.0.0.1`을 권장합니다. 로컬 환경에 따라 `localhost`가 IPv6 또는 다른 resolver 경로를 타면서 진단이 어려워질 수 있습니다.

기본 Docker 앱 포트는 `18080`입니다. IDE나 `bootRun`이 `8080`을 이미 쓰고 있어도 부하 테스트 대상이 Docker 앱으로 고정되게 하기 위한 기본값입니다. 다른 포트를 쓰고 싶으면 `APP_HOST_PORT`로 덮어쓸 수 있습니다.

기본 접속 정보:

- 앱: `http://localhost:18080`
- 앱 권장 BASE_URL: `http://127.0.0.1:18080`
- InfluxDB: `http://localhost:8086`
- Grafana: `http://localhost:3000`
- Grafana 계정: `admin / admin`

`ADMIN_PASSWORD`처럼 `!`가 포함된 값은 셸 해석 이슈를 피하려고 따옴표로 감싸는 편이 안전합니다.

Slack 보고를 같이 쓰려면 [load-test/k6/.env](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/.env)에 `LOAD_TEST_SLACK_WEBHOOK=` 값을 넣고, plain `k6 run` 대신 [run-with-slack.mjs](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/run-with-slack.mjs)로 실행합니다.
현재 로컬 기본값은 `LOAD_TEST_SLACK_NOTIFY_ON=always`라서, 성공과 실패 모두 Slack으로 전송됩니다.

왜 `InfluxDB`를 쓰는지:

- `k6`는 `InfluxDB`로 메트릭을 바로 내보낼 수 있어서 실시간 시계열 수집 경로가 단순합니다.
- 실행이 끝난 뒤 summary만 보는 대신, 실행 중 `p50/p95/p99`, 실패율, 처리량, VU 변화를 시간축으로 바로 확인할 수 있습니다.
- 앱 메트릭은 `/actuator/prometheus`, 부하 테스트 메트릭은 `InfluxDB`로 분리해서 보면 원인 분석이 더 단순해집니다.
- 반복 실행 결과를 Grafana에서 같은 방식으로 누적해 비교하기 쉽습니다.

## 2. 대시보드 열기

1. 브라우저에서 `http://localhost:3000` 접속
2. `admin / admin` 로그인
3. 왼쪽 메뉴에서 `Dashboards`
4. `k6` 폴더 진입
5. `k6 Overview` 열기

실행 전에 아래 상태를 먼저 봅니다.

- 우상단 time range: `Last 15 minutes`
- refresh interval: `5s` 또는 `10s`
- `Scenario` 변수: 기본은 `All`

대시보드가 비어 있어도 정상입니다. 아직 `k6 --out influxdb=...` 실행 전이면 데이터가 없습니다.

## 3. 테스트 실행

### Smoke

```bash
node load-test/k6/run-with-slack.mjs smoke --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ADMIN_EMAIL=loadtest-admin@coupon.local \
  -e ADMIN_PASSWORD='admin1234!'
```

### Issue Request Smoke

```bash
node load-test/k6/run-with-slack.mjs issue-request-smoke --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ADMIN_EMAIL=loadtest-admin@coupon.local \
  -e ADMIN_PASSWORD='admin1234!'
```

### Baseline

```bash
node load-test/k6/run-with-slack.mjs baseline --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e BASELINE_VUS=20 \
  -e BASELINE_DURATION=10m
```

### Issue Burst

정확히 `1000명 동시 발급`과 재고 정합성을 같이 확인하는 전용 시나리오입니다.

모든 사용자가 성공해야 하는 기본 케이스:

```bash
node load-test/k6/run-with-slack.mjs issue-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_BURST_VUS=1000 \
  -e ISSUE_BURST_STOCK=1000 \
  -e ISSUE_BURST_LOCK_RETRY_COUNT=3 \
  -e ISSUE_BURST_LOCK_RETRY_DELAY_MS=250
```

재고보다 요청이 많을 때도 서버가 터지지 않고 제어되는지 보는 케이스:

```bash
node load-test/k6/run-with-slack.mjs issue-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_BURST_VUS=1000 \
  -e ISSUE_BURST_STOCK=900 \
  -e ISSUE_BURST_LOCK_RETRY_COUNT=3 \
  -e ISSUE_BURST_LOCK_RETRY_DELAY_MS=250
```

이 시나리오는 실행 뒤 아래를 자동으로 다시 확인합니다.

- 최종 발급 건수
- 최종 잔여 재고
- `발급 건수 + 잔여 재고 == 초기 재고` 정합성
- `min(VU, 재고)`와 실제 결과가 일치하는지
- 서버 오류가 없었는지

추가 조정값:

- `ISSUE_BURST_LOCK_RETRY_COUNT`
  - `429 LOCK_ACQUISITION_FAILED` 응답을 받았을 때 같은 사용자가 다시 시도하는 횟수입니다.
- `ISSUE_BURST_LOCK_RETRY_DELAY_MS`
  - 재시도 사이 기본 대기 시간입니다. 재시도 횟수에 따라 점진적으로 늘어납니다.
- Slack과 summary의 `재시도 시도 횟수`는 실제로 `429 LOCK_ACQUISITION_FAILED`를 받아 재시도를 시작한 횟수입니다.

### Contention

```bash
node load-test/k6/run-with-slack.mjs contention --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e CONTENTION_VUS=100
```

`contention`은 발급 락 경합을 보기 위한 시나리오라서, 사용자 회원가입과 로그인은 `setup()`에서 먼저 처리하고 실제 VU 실행 단계에서는 `issueCoupon`만 동시에 호출합니다.
`issue-burst`는 VU 수만큼 테스트 사용자를 `setup()`에서 먼저 준비하고, 본 실행에서는 각 사용자가 정확히 1회씩 동시에 발급 요청합니다.
`issue-burst`는 `1000명` 기준으로 setup 시간이 수십 초에서 수 분 걸릴 수 있습니다. 이 시간은 데이터 준비 시간입니다.

### Issue Overload

```bash
node load-test/k6/run-with-slack.mjs issue-overload --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_OVERLOAD_VUS=100 \
  -e ISSUE_OVERLOAD_DURATION=10m \
  -e ISSUE_OVERLOAD_USER_POOL_SIZE=200 \
  -e ISSUE_OVERLOAD_COUPON_POOL_SIZE=500
```

이 시나리오는 `coupon-issue`만 지속적으로 과부하하기 위한 전용 시나리오입니다.

- `ISSUE_OVERLOAD_VUS`
  - 동시에 계속 요청을 보내는 사용자 수입니다.
- `ISSUE_OVERLOAD_DURATION`
  - 과부하를 유지할 시간입니다.
- `ISSUE_OVERLOAD_USER_POOL_SIZE x ISSUE_OVERLOAD_COUPON_POOL_SIZE`
  - 중복 없이 발급 가능한 총 조합 수입니다.

예시의 경우 capacity는 `200 x 500 = 100,000`건입니다. 테스트 도중 이 용량을 넘기면 `issue_overload capacity exhausted ...` 메시지로 테스트를 바로 abort 하며, 원인과 늘려야 할 env var 이름을 같이 알려줍니다.

### Issue Request Burst

Kafka accepted-model 경로의 동시성 검증은 아래 시나리오를 사용합니다.

```bash
node load-test/k6/run-with-slack.mjs issue-request-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_REQUEST_BURST_VUS=1000 \
  -e ISSUE_REQUEST_BURST_STOCK=1000 \
  -e ISSUE_REQUEST_POLL_TIMEOUT_SECONDS=30 \
  -e ISSUE_REQUEST_POLL_INTERVAL_MS=500
```

재고보다 더 많은 요청을 넣는 제어 케이스:

```bash
node load-test/k6/run-with-slack.mjs issue-request-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_REQUEST_BURST_VUS=1000 \
  -e ISSUE_REQUEST_BURST_STOCK=900 \
  -e ISSUE_REQUEST_POLL_TIMEOUT_SECONDS=30 \
  -e ISSUE_REQUEST_POLL_INTERVAL_MS=500
```

이 시나리오는 다음을 같이 검증합니다.

- request가 모두 `202 Accepted`로 정상 접수되는지
- 각 request가 `SUCCEEDED` 또는 `FAILED(OUT_OF_STOCK)`로 수렴하는지
- `DEAD` 요청이 없는지
- 최종 `issued + remaining == initial stock` 정합성이 유지되는지

쿠폰 1개 재고 케이스를 고정해서 반복 확인하려면 아래 전용 런북을 사용합니다.

```bash
./load-test/k6/run-single-coupon-stock-runbook.sh exact
./load-test/k6/run-single-coupon-stock-runbook.sh oversubscribed
./load-test/k6/run-single-coupon-stock-runbook.sh scarce
./load-test/k6/run-single-coupon-stock-runbook.sh single-stock
```

- `exact`
  - 사용자 1,000명 / 쿠폰 1개 / 재고 1,000개
  - 기대: 1,000건 성공, 잔여 재고 0
- `oversubscribed`
  - 사용자 1,000명 / 쿠폰 1개 / 재고 900개
  - 기대: 900건 성공, 100건 `OUT_OF_STOCK`
- `scarce`
  - 사용자 1,000명 / 쿠폰 1개 / 재고 100개
  - 기대: 100건 성공, 900건 `OUT_OF_STOCK`
- `single-stock`
  - 사용자 1,000명 / 쿠폰 1개 / 재고 1개
  - 기대: 1건 성공, 999건 `OUT_OF_STOCK`

### Issue Request Overload

Kafka relay/consumer를 포함한 end-to-end 과부하는 아래 시나리오를 사용합니다.

```bash
node load-test/k6/run-with-slack.mjs issue-request-overload --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_REQUEST_OVERLOAD_VUS=50 \
  -e ISSUE_REQUEST_OVERLOAD_DURATION=10m \
  -e ISSUE_REQUEST_OVERLOAD_COUPON_POOL_SIZE=200 \
  -e ISSUE_REQUEST_OVERLOAD_STOCK_PER_COUPON=100000 \
  -e ISSUE_REQUEST_POLL_TIMEOUT_SECONDS=30 \
  -e ISSUE_REQUEST_POLL_INTERVAL_MS=500
```

이 시나리오는 `POST /coupon-issue-requests`만 보내고 끝내지 않습니다. 각 요청이 최종 상태로 수렴할 때까지 polling하므로, Kafka relay/consumer를 포함한 실제 비동기 발급 파이프라인의 처리량과 tail latency를 같이 볼 수 있습니다.

### Issue Request Ramp

비교 대상 저장소의 `202 Accepted` 중심 부하 테스트와 가장 가까운 시나리오입니다.

```bash
node load-test/k6/run-with-slack.mjs issue-request-ramp --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080
```

기본 stage는 아래 형태입니다.

- `0 -> 3000`
- `3000 유지`
- `5000 상승`
- `5000 유지`
- `7000 상승`
- `7000 유지`
- `0으로 종료`

이 시나리오는 synthetic `userId`를 매 iteration마다 새로 만들고, request acceptance만 측정합니다.

- 보는 것:
  - `202 Accepted`
  - request intake latency
  - error rate
- 보지 않는 것:
  - request가 최종 `SUCCEEDED`까지 갔는지
  - 재고 정합성

즉, `issue-request-ramp`는 intake 성능 확인용이고, `issue-request-burst`와 `issue-request-overload`가 end-to-end 정합성 확인용입니다.

### Issue Request Real Ramp

실사용자 가정으로 request intake 성능을 보려면 아래 시나리오를 사용합니다.

```bash
node load-test/k6/run-with-slack.mjs issue-request-real-ramp --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_REQUEST_REAL_RAMP_USER_POOL_SIZE=7000 \
  -e ISSUE_REQUEST_REAL_RAMP_COUPON_POOL_SIZE=1000 \
  -e ISSUE_REQUEST_REAL_RAMP_STOCK_PER_COUPON=100000
```

이 시나리오는 synthetic endpoint가 아니라 실제 `/coupon-issue-requests`를 호출합니다.

- setup에서 사용자 회원가입/로그인 수행
- access token 기반 호출
- request status polling은 하지 않음
- intake latency와 오류율 위주로 확인

즉, 비교 대상 저장소의 `202 Accepted` 중심 테스트 의도와 가장 가깝지만, setup 비용이 훨씬 큽니다.

중요:

- 실시간 대시보드를 보려면 반드시 `--out influxdb=http://localhost:8086/myk6db`를 붙입니다.
- `--out` 없이 실행하면 JSON summary만 남고 Grafana에는 안 보입니다.
- `run-with-slack.mjs`는 `load-test/k6/.env`를 자동으로 읽고, 실패 시 Slack webhook으로 메세지를 보냅니다.
- 앱이 막 기동된 상태라면 `setup()`이 health와 admin signin을 몇 초간 대기할 수 있습니다.

## 3-1. Request Breakdown 대시보드

`Request Breakdown`은 `k6 Overview`와 별도로 요청 이름 기준 집계를 보는 대시보드입니다.

여기서는 `request_group=business` 태그가 붙은 요청만 집계합니다.

- 포함:
  - `issue_coupon`
  - `issue_coupon_request`
  - `get_coupon_issue_request`
  - `use_coupon`
  - `cancel_coupon`
  - `get_coupon`
  - `get_coupon_issue_page`
  - `get_my_coupons`
- 제외:
  - `signup`
  - `signin`
  - `admin bootstrap`
  - `health`
  - `create_coupon`
  - `activate_coupon`

확인 순서:

1. Grafana에서 `k6 / Request Breakdown` 열기
2. 우상단 time range를 방금 실행 시간대로 맞추기
3. `Scenario` 변수를 현재 실행 시나리오로 좁히기
4. 아래 패널을 순서대로 보기

- `Request p95 Over Time`
  - 요청별 tail latency 변화를 시간축으로 봅니다.
- `Request p95 Latency`
  - 어떤 요청이 지속적으로 느린지 빠르게 비교합니다.
- `Request Max Latency`
  - 순간 최대 지연이 어느 요청에서 나왔는지 봅니다.
- `Request Error Rate`
  - 오류가 특정 요청에 몰렸는지 확인합니다.
- `Request Count`
  - 실제 트래픽이 어느 요청에 집중됐는지 봅니다.
- `Top Slow Request Groups`
  - 느린 요청군을 요약 테이블로 확인합니다.

중요:

- Grafana는 집계용입니다.
- 가장 느린 단일 요청의 raw response preview는 결과 artifact에서 확인합니다.
- artifact 위치:
  - `load-test/k6/results/<scenario>-slow-requests-latest.json`
  - `load-test/k6/results/<scenario>-slow-requests-<timestamp>.json`
- 이 artifact는 plain `k6 run`이 아니라 `node load-test/k6/run-with-slack.mjs ...` 실행 시 생성됩니다.

## 3-2. Slack 알림 확인

실패가 나면 Slack에는 아래 형식으로 전송됩니다.

```text
[local 환경 부하 테스트 내용]
[부하테스트 내용 보고]
- 테스트 종류: 쿠폰 발급 API 과부하
- 결과: 실패
- 한 줄 요약: 테스트에 쓸 사용자 또는 쿠폰 수가 부족해서 중간에 멈췄습니다.
- 부하 조건: 100명이 10분 동안 coupon-issue를 반복 호출
- 상세 설정: ISSUE_OVERLOAD_VUS=100, ISSUE_OVERLOAD_DURATION=10m, ...
- 실행 시간: ...
- 상세 설명: 현재 준비된 테스트 데이터는 사용자 200명, 쿠폰 500개 수준입니다.
- 다음 조치: ISSUE_OVERLOAD_USER_POOL_SIZE 또는 ISSUE_OVERLOAD_COUPON_POOL_SIZE를 늘린 뒤 다시 실행해 주세요.
- 기준 미달 항목: 오류율 기준 미달
- 응답속도 중앙값(p50): ...
- 느린 요청 기준(p95): ...
- 매우 느린 요청 기준(p99): ...
- 오류율: ...
- 정상 응답 확인율: ...
- 결과 파일: load-test/k6/results/issue-overload-latest.json
```

성공 메세지도 같은 구조로 전달됩니다.

```text
[local 환경 부하 테스트 내용]
[부하테스트 내용 보고]
- 테스트 종류: 일반 사용량 기준 부하
- 결과: 성공
- 한 줄 요약: 일반 사용량 기준 부하를 안정적으로 처리했습니다.
- 부하 조건: 20명이 10m 동안 일반 사용 패턴으로 실행
- 상세 설정: BASELINE_VUS=20, BASELINE_DURATION=10m
- 실행 시간: ...
- 상세 설명: 테스트 중 오류율과 정상 응답 확인율이 기준 안에 들었고, 시나리오를 끝까지 수행했습니다.
- 다음 조치: 이번 결과를 기준선으로 기록하고, 이전 실행 대비 p95/p99 변화가 없는지 비교해 주세요.
- 응답속도 중앙값(p50): ...
- 느린 요청 기준(p95): ...
- 매우 느린 요청 기준(p99): ...
- 오류율: ...
- 정상 응답 확인율: ...
- 결과 파일: load-test/k6/results/baseline-latest.json
```

Webhook이 비어 있거나 Slack 전송을 끄고 싶으면:

- `LOAD_TEST_SLACK_WEBHOOK=` 비워두기
- 또는 `LOAD_TEST_SLACK_NOTIFY_ON=never`

실패한 경우만 Slack으로 받고 싶으면:

- `LOAD_TEST_SLACK_NOTIFY_ON=failure`

이 경우에도 preview는 아래 파일에 남습니다.

- `load-test/k6/results/<scenario>-slack-latest.txt`

## 4. 실행 중 확인 포인트

`k6 Overview`는 아래 패널을 기준으로 봅니다.

- `HTTP Request Duration p50`
  - 일반적인 응답시간 중앙값입니다.
  - 전체 시스템의 평상시 체감 속도가 흔들리는지 볼 때 유용합니다.
- `HTTP Request Duration p95`
  - tail latency의 대표값입니다.
  - 완만하게 유지되면 정상, 계단식 상승이면 포화 구간 가능성이 큽니다.
- `HTTP Request Duration p99`
  - 가장 느린 꼬리 구간입니다.
  - 락 경합, DB 지연, retry 같은 이상치가 실제로 얼마나 심한지 볼 때 유용합니다.
- `HTTP Request Failed Rate`
  - 0에 가까워야 합니다.
  - 짧은 스파이크는 있을 수 있지만 지속적으로 오르면 병목 또는 오류입니다.
- `Checks Pass Rate`
  - 스크립트 assertion 성공률입니다.
  - 1.0 아래로 내려가면 응답 상태나 envelope가 깨지고 있다는 뜻입니다.
- `Iterations`
  - 시간당 처리량 흐름입니다.
  - VU를 유지하는데 iterations가 눌리면 서버가 밀리기 시작한 신호입니다.
- `Virtual Users`
  - 의도한 부하가 실제로 걸렸는지 확인합니다.
  - 설정한 VU가 유지되지 않으면 실행 조건이나 executor 설정부터 봅니다.

## 5. 테스트가 끝난 뒤 해석 방법

### Smoke

목표:

- 기능이 끊기지 않는지 확인
- 인증, 쿠폰 생성, 발급, 사용 플로우가 한 번에 닫히는지 확인

정상 신호:

- failed rate 거의 0
- checks rate 거의 1
- p95가 짧고 급등이 없음

이상 신호:

- 초반부터 failed rate가 높음
- checks rate가 즉시 깨짐
- dashboard에는 데이터가 있는데 smoke가 실패하면 기능 회귀 가능성이 큼

### Issue Request Smoke

목표:

- `POST /coupon-issue-requests`가 정상적으로 `202 Accepted`를 반환하는지 확인
- request status polling이 최종 `SUCCEEDED`까지 수렴하는지 확인
- Kafka relay/consumer가 기본 경로에서 정상 동작하는지 확인

정상 신호:

- failed rate 거의 0
- checks rate 거의 1
- request가 최종 `SUCCEEDED`
- `couponIssueId`가 채워짐

이상 신호:

- request는 접수되지만 polling timeout 발생
- request가 `DEAD`로 떨어짐
- request가 `FAILED`로 끝나는데 테스트 데이터상 비즈니스 실패 이유가 없음

이 경우에는 아래를 먼저 봅니다.

- `t_coupon_issue_request`
- `t_outbox_event`
- Kafka UI topic 적재 여부
- worker 로그

### Baseline

목표:

- steady-state에서 성능 기준선 확보
- 장시간 실행 중 tail latency와 처리량 안정성 확인

정상 신호:

- p50이 큰 흔들림 없이 유지
- p95가 큰 흔들림 없이 유지
- p99가 짧게 튀어도 빠르게 회복
- failed rate가 장시간 0 근처
- iterations가 시간축에서 비교적 일정
- VU는 고정, iterations도 유지

이상 신호:

- p50까지 같이 상승
- 시간이 갈수록 p95가 계속 상승
- p99가 길게 벌어지며 회복되지 않음
- failed rate가 후반부로 갈수록 증가
- VU는 유지되는데 iterations가 눌림

이 경우에는 같은 시간대의 아래를 같이 봅니다.

- `/actuator/prometheus`
- 애플리케이션 로그
- MySQL/Redis 컨테이너 상태

### Issue Burst

목표:

- `1000명` 같은 대규모 사용자가 같은 쿠폰을 동시에 발급해도 재고 정합성이 깨지지 않는지 확인
- 서버가 5xx로 터지지 않고 비즈니스 오류로만 제어되는지 확인

정상 신호:

- `issue_burst_server_error_count = 0`
- `issue_burst_unexpected_client_error_count = 0`
- `issue_burst_integrity_ok = 100%`
- `issue_burst_expected_result_ok = 100%`
- `issue_burst_retryable_lock_failure_count`는 0이 아니어도 괜찮고, 최종적으로 `서버 오류 0 / 예상 밖 클라이언트 오류 0`이면 제어된 경쟁으로 봅니다.
- `ISSUE_BURST_STOCK=1000`, `ISSUE_BURST_VUS=1000`이면:
  - 성공 발급 건수 `1000`
  - 최종 잔여 재고 `0`
- `ISSUE_BURST_STOCK=900`, `ISSUE_BURST_VUS=1000`이면:
  - 성공 발급 건수 `900`
  - 재고 부족 건수 `100`
  - 최종 잔여 재고 `0`

이상 신호:

- `issue_burst_integrity_ok`가 100%가 아님
- `issue_burst_expected_result_ok`가 100%가 아님
- `issue_burst_server_error_count`가 1 이상
- `http_req_failed`가 급격히 올라가고, 같은 시간대에 앱 로그에 DB/락/커넥션 풀 오류가 보임
- `issue_burst_unexpected_client_error_count`가 1 이상인데 `issue_burst_retryable_lock_failure_count`도 같이 높음

원인 보는 순서:

1. Slack 또는 결과 JSON에서 `issue_burst_server_error_count`, `issue_burst_unexpected_client_error_count`를 먼저 확인
2. `issue_burst_retryable_lock_failure_count`가 높으면 락 경합이 있었지만 재시도로 흡수했는지 같이 확인
3. `issue_burst_final_issued_count`, `issue_burst_final_remaining_quantity`를 보고 재고 합계가 맞는지 확인
4. Grafana에서 같은 시간대 `p95`, `p99`, `HTTP Request Failed Rate`를 확인
5. 동시에 앱 로그와 `/actuator/prometheus`에서 DB 커넥션 풀, 스레드, 에러 수치를 확인
6. `Request Breakdown`에서 어느 요청명이 실제로 느렸는지 확인
7. `load-test/k6/results/issue-burst-slow-requests-latest.json`에서 상위 느린 요청의 `responsePreview`를 확인

빠른 판정:

- `정합성 통과 + 서버 오류 0`
  - 재고 동시성 제어는 정상으로 판단 가능
- `lock retry가 있었지만 최종 결과 정상`
  - 락 경합은 있었지만 시스템이 흡수했다고 판단 가능
- `정합성 깨짐 또는 서버 오류 존재`
  - 즉시 원인 분석이 필요한 상태

### Issue Request Burst

목표:

- accepted-model 경로에서 대량 동시 요청이 `SUCCEEDED` 또는 `FAILED(OUT_OF_STOCK)`로만 수렴하는지 확인
- Kafka relay/consumer를 거친 뒤에도 재고 정합성이 깨지지 않는지 확인
- `DEAD` 요청 없이 request 상태가 모두 명시적으로 수렴하는지 확인

정상 신호:

- `issue_request_burst_dead_count = 0`
- `issue_request_burst_unexpected_failure_count = 0`
- `issue_request_burst_integrity_ok = 100%`
- `issue_request_burst_expected_result_ok = 100%`
- `ISSUE_REQUEST_BURST_STOCK=1000`, `ISSUE_REQUEST_BURST_VUS=1000`이면:
  - 성공 발급 건수 `1000`
  - 최종 잔여 재고 `0`
- `ISSUE_REQUEST_BURST_STOCK=900`, `ISSUE_REQUEST_BURST_VUS=1000`이면:
  - 성공 발급 건수 `900`
  - 재고 부족 건수 `100`
  - 최종 잔여 재고 `0`

이상 신호:

- `issue_request_burst_dead_count`가 1 이상
- `issue_request_burst_unexpected_failure_count`가 1 이상
- `issue_request_burst_integrity_ok`가 100%가 아님
- `coupon issue request ... did not reach terminal status` timeout 발생

원인 보는 순서:

1. `t_coupon_issue_request`에서 `PENDING/ENQUEUED/PROCESSING/DEAD` 비율 확인
2. `t_outbox_event`에서 relay backlog와 failed/dead 개수 확인
3. Kafka UI에서 topic lag와 DLQ 적재 여부 확인
4. worker 로그에서 relay publish 실패, consumer retry, DLQ 처리 로그 확인
5. `issued + remaining == initial stock`가 깨졌다면 DB 상태를 먼저 확인

### Issue Request Overload

목표:

- accepted-model + Kafka relay/consumer 경로의 장시간 처리량과 tail latency를 확인
- 각 요청이 최종 `SUCCEEDED`까지 수렴하는지 확인
- sustained load에서 relay/consumer/backlog가 밀리지 않는지 확인

정상 신호:

- `issue_request_overload_dead_count = 0`
- `issue_request_overload_unexpected_failure_count = 0`
- p95/p99가 시간축에서 과도하게 상승하지 않음
- iterations가 장시간 비교적 일정

이상 신호:

- request terminal timeout 반복
- `issue_request_overload_dead_count`가 증가
- `issue_request_overload_unexpected_failure_count`가 증가
- p95/p99는 오르는데 iterations가 눌림

이 경우에는 아래를 같이 봅니다.

- worker `/actuator/health`
- Kafka UI lag
- `t_coupon_issue_request` oldest `ENQUEUED` / `PROCESSING`
- reconciliation 관련 로그와 메트릭

### Issue Request Ramp

목표:

- Kafka accepted-model intake 성능을 고부하에서 확인
- request acceptance latency와 오류율만 측정
- 인증/회원가입 비용 없이 intake 자체만 집중적으로 보기

정상 신호:

- checks rate 거의 1
- failed rate 거의 0
- p95가 임계값 이내
- accepted count가 stage 상승에 맞춰 꾸준히 증가

이상 신호:

- `issue_request_ramp accepted` check 실패
- 5xx 또는 timeout 증가
- p95/p99는 오르는데 iterations가 급격히 눌림

원인 보는 순서:

1. API 서버 CPU/스레드/DB connection 상태 확인
2. `t_coupon_issue_request` insert 처리량 확인
3. `t_outbox_event` 적재 속도와 backlog 확인
4. Kafka relay backlog와 worker lag는 참고로만 보고, 이 시나리오에서는 acceptance latency를 먼저 본다

### Issue Request Real Ramp

목표:

- 실사용자 인증 세션 기준으로 request intake 성능 확인
- synthetic endpoint가 아니라 실제 `/coupon-issue-requests`를 통한 acceptance latency 확인
- 회원가입/로그인 이후 access token 호출 환경에서 병목 확인

정상 신호:

- checks rate 거의 1
- failed rate 거의 0
- p95가 임계값 이내
- accepted count가 stage 상승에 맞춰 꾸준히 증가

이상 신호:

- `issue_request_real_ramp accepted` check 실패
- `issue_request_real_ramp capacity exhausted`
- signup/signin setup 시간이 과도하게 길어짐
- p95/p99는 오르는데 iterations가 급격히 눌림

원인 보는 순서:

1. 사용자 풀/쿠폰 풀 capacity 먼저 확인
2. signup/signin setup 시간이 병목인지 확인
3. API 서버 CPU/스레드/DB connection 상태 확인
4. `t_coupon_issue_request` insert 처리량과 `t_outbox_event` 적재 속도 확인
- `정합성 통과 + 재시도 가능한 락 실패만 존재`
  - hot coupon 경합은 있었지만 최종 결과는 제어됨
- `정합성 통과 + p95/p99만 상승`
  - 락 경합은 있지만 데이터는 안전
- `정합성 실패`
  - 재고 차감 또는 상태 전이 설계에 문제 가능성
- `서버 오류 발생`
  - DB 풀, 락 대기, 앱 리소스 포화 가능성 우선 점검

### Contention

목표:

- 동일 쿠폰 동시 발급 시 정합성과 병목 구간 확인

정상 신호:

- p95, p99 상승은 있어도 checks rate는 크게 깨지지 않음
- iterations가 빠르게 몰리면서도 실패율이 통제됨

이상 신호:

- p95, p99 급등과 함께 failed rate가 같이 오름
- checks rate가 떨어짐
- iterations가 급격히 눌리거나 plateau가 생김

주의:

- contention은 baseline보다 latency spike가 더 자연스럽습니다.
- 중요한 건 spike 자체보다, 그 spike가 지속되는지와 실패율이 함께 올라가는지입니다.

### Issue Overload

목표:

- `coupon-issue`만 장시간 과부하했을 때 처리량, p95, p99, 실패율이 어떻게 변하는지 확인
- 순간 경합이 아니라 지속 부하에서 발급 API가 얼마나 오래 버티는지 확인

정상 신호:

- `Virtual Users`는 설정한 값으로 유지
- `Iterations`가 시간축에서 크게 꺾이지 않음
- `p50`은 흔들려도 `p95`, `p99`가 회복 가능한 범위 안에 머묾
- `failed rate`가 장시간 0 근처 또는 낮은 수준에서 통제됨

이상 신호:

- `p95`, `p99`가 시간이 갈수록 계속 상승
- `Iterations`가 점점 눌리거나 plateau를 형성
- `failed rate`가 후반부로 갈수록 올라감
- `issue_overload capacity exhausted`가 뜸

원인 보는 순서:

1. `issue_overload capacity exhausted`가 먼저 떴는지 확인
2. 아니면 `issue_coupon` 응답 코드가 4xx인지 5xx인지 확인
3. 같은 시간대 `p95/p99`, failed rate, `/actuator/prometheus`, 앱 로그를 같이 확인

빠른 해석 기준:

- `capacity exhausted`
  - 테스트 데이터 조합 부족
- 4xx 증가
  - 중복 발급, 비활성/만료 쿠폰 같은 비즈니스 조건 문제
- 5xx 증가 + p95/p99 상승
  - DB, 락, 스레드, 연결 풀 등 실제 서버 병목 가능성

## 6. 앱 메트릭과 함께 보기

현재 Grafana에는 `k6` 메트릭만 들어갑니다. 앱 메트릭은 별도로 확인합니다.

추천 흐름:

1. Grafana에서 `k6` 패널로 부하 구간 확인
2. 같은 시간 범위에 맞춰 `/actuator/prometheus` 또는 로그 확인
3. 아래 관점으로 연결해서 해석

- p95, p99 상승 시점과 Tomcat thread/connection 포화 시점이 겹치는지
- failed rate 상승 시점과 애플리케이션 예외 로그가 겹치는지
- iterations 저하 시점과 DB/Redis 응답 저하가 겹치는지

## 7. 흔한 문제

### Grafana에 데이터가 안 보임

확인 순서:

1. `k6 run`에 `--out influxdb=http://localhost:8086/myk6db`가 붙었는지
2. `curl -i http://localhost:8086/ping` 응답이 오는지
3. Grafana time range가 너무 좁지 않은지
4. `Scenario` 필터가 특정 값으로 고정돼 있지 않은지

### 대시보드는 열리는데 패널이 비어 있음

확인 순서:

1. 아직 테스트가 시작되지 않았는지
2. 방금 실행한 시나리오와 time range가 안 맞는지
3. datasource `k6-influxdb`가 살아 있는지

### 앱 요청이 실패함

확인 순서:

1. `curl http://127.0.0.1:18080/ping`
2. `curl http://127.0.0.1:18080/actuator/health`
3. `curl -X POST http://127.0.0.1:18080/load-test/admin/signin -H 'Content-Type: application/json' -d '{}'`
4. `k6` 실행 시 `-e BASE_URL=http://127.0.0.1:18080`를 썼는지
5. `ADMIN_PASSWORD`를 `'admin1234!'`처럼 따옴표로 감쌌는지
6. 앱 이미지를 최근 코드 기준으로 다시 빌드했는지 (`docker compose ... up --build`)
7. admin 계정이 local bootstrap으로 생성됐는지
8. 기존 MySQL volume 때문에 `coupon-admin` 계정이 꼬이지 않았는지

필요하면 한 번 초기화합니다.

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose \
  -f docker/docker-compose.yml \
  -f docker/docker-compose.k6-observability.yml \
  up --build
```

## 8. 종료

테스트와 대시보드 확인이 끝났으면 내립니다.

```bash
docker compose \
  -f docker/docker-compose.yml \
  -f docker/docker-compose.k6-observability.yml \
  down
```

볼륨까지 정리하려면:

```bash
docker compose \
  -f docker/docker-compose.yml \
  -f docker/docker-compose.k6-observability.yml \
  down -v
```
