# k6 Load Test Baseline

이 디렉터리는 쿠폰 시스템의 첫 부하 테스트 baseline을 위한 `k6` 시나리오를 담고 있습니다.

대시보드 오픈부터 실행, 확인, 해석까지 한 번에 따라가려면 [RUNBOOK.md](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/RUNBOOK.md)를 같이 봅니다.

## 시나리오

- `smoke.js`
  - 관리자 로그인, 쿠폰 생성, 사용자 회원가입/로그인, 쿠폰 발급, 쿠폰 사용까지 한 번에 검증합니다.
- `baseline.js`
  - VU별 사용자 세션을 재사용하면서 `issue/use`, `issue/cancel`, `my-coupons`를 혼합 호출합니다.
- `contention.js`
  - 동일 쿠폰에 동시에 발급 요청을 몰아 정합성과 병목을 확인합니다.
  - 인증 경합이 결과를 왜곡하지 않도록, 사용자와 토큰은 `setup()`에서 먼저 준비하고 본 실행에서는 발급 호출만 동시에 보냅니다.

## 사전 준비
- `issue-overload.js`
  - `coupon-issue`만 N분 동안 M명이 쉬지 않고 호출하는 지속 과부하 시나리오입니다.
  - `contention`과 달리 순간 동시성 1회가 아니라, 일정 시간 동안 발급 API 처리량과 tail latency가 계속 버티는지 봅니다.
  - 중복 발급으로 결과가 오염되지 않도록 `사용자 풀 x 쿠폰 풀` 조합으로 유니크한 발급 대상을 만듭니다.

1. 로컬 스택을 올립니다.

```bash
docker compose -f docker/docker-compose.yml up --build
```

기본 Docker 앱 포트는 `18080`입니다. IDE나 `bootRun`이 `8080`을 이미 쓰고 있어도 부하 테스트 대상과 충돌하지 않게 하기 위한 기본값입니다. 다른 포트를 쓰고 싶으면 `APP_HOST_PORT`로 덮어쓸 수 있습니다.

실시간 `k6 -> InfluxDB -> Grafana` 관측이 필요하면 overlay compose를 함께 올립니다.

```bash
docker compose \
  -f docker/docker-compose.yml \
  -f docker/docker-compose.k6-observability.yml \
  up --build
```

2. 애플리케이션이 준비될 때까지 기다립니다.

```bash
curl http://127.0.0.1:18080/ping
curl http://127.0.0.1:18080/actuator/health
curl -X POST http://127.0.0.1:18080/load-test/admin/signin \
  -H 'Content-Type: application/json' \
  -d '{}'
```

현재 스크립트는 `setup()`에서 `/actuator/health`를 확인한 뒤, local profile 전용 `/load-test/admin/signin`으로 관리자 계정 보장과 토큰 발급을 먼저 시도합니다. 구버전 앱 이미지에서는 이 경로가 없을 수 있으니, 관련 코드를 반영한 뒤에는 `docker compose ... up --build`로 앱 이미지를 다시 만드는 편이 안전합니다.

`k6` 실행 예시의 `BASE_URL`은 `localhost` 대신 `127.0.0.1`을 사용합니다. 로컬 환경에 따라 `localhost`가 IPv6 또는 다른 resolver 경로를 타면서 진단이 어려워질 수 있습니다.

`ADMIN_PASSWORD`처럼 `!`가 포함된 값은 셸 해석 이슈를 피하려고 반드시 따옴표로 감싸는 편이 안전합니다.

기본 local profile에서는 애플리케이션 기동 시 아래 관리자 계정을 자동 생성합니다.

- 이메일: `loadtest-admin@coupon.local`
- 비밀번호: `admin1234!`

기존 MySQL volume이 이미 존재해서 `coupon-admin` 계정이 생성되지 않았다면, 한 번만 볼륨을 초기화한 뒤 다시 올려야 합니다.

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up --build
```

## Optional Observability

overlay compose를 사용하면 아래 로컬 엔드포인트가 추가됩니다.

- Grafana: `http://localhost:3000`
- InfluxDB v1: `http://localhost:8086`

Grafana 기본 계정은 아래와 같습니다.

- 아이디: `admin`
- 비밀번호: `admin`

Grafana는 repo-local provisioning을 통해 아래를 자동 등록합니다.

- datasource: `k6-influxdb`
- dashboard folder: `k6`
- dashboard: `k6 Overview`

왜 `InfluxDB`를 같이 쓰는지:

- `k6`는 실행 중 메트릭을 `InfluxDB`로 바로 밀어 넣을 수 있어서, 추가 exporter 없이도 실시간 시계열 대시보드를 만들기 쉽습니다.
- 콘솔 summary나 JSON summary만으로는 실행 중 추세를 보기 어렵지만, `InfluxDB + Grafana`를 쓰면 latency, 실패율, 처리량 변화를 시간축으로 바로 볼 수 있습니다.
- 여러 번의 테스트 실행 결과를 같은 대시보드에서 비교하기 쉽습니다. baseline과 contention을 같은 축에서 보는 데 유리합니다.
- 앱 메트릭은 기존 `/actuator/prometheus`가 담당하고, 부하 테스트 메트릭은 `InfluxDB`가 담당하게 나누면 역할이 명확해집니다.

현재 범위에서 Grafana는 `k6` 시계열만 시각화합니다. 애플리케이션 메트릭은 기존 `/actuator/prometheus` 경로를 별도로 확인합니다.

## 실행 예시

```bash
k6 run \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ADMIN_EMAIL=loadtest-admin@coupon.local \
  -e ADMIN_PASSWORD='admin1234!' \
  load-test/k6/smoke.js
```

실시간 InfluxDB 출력까지 같이 보려면 `--out influxdb=...`를 추가합니다.

```bash
k6 run \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ADMIN_EMAIL=loadtest-admin@coupon.local \
  -e ADMIN_PASSWORD='admin1234!' \
  load-test/k6/smoke.js
```

```bash
k6 run \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e BASELINE_VUS=20 \
  -e BASELINE_DURATION=10m \
  load-test/k6/baseline.js
```

```bash
k6 run \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e BASELINE_VUS=20 \
  -e BASELINE_DURATION=10m \
  load-test/k6/baseline.js
```

```bash
k6 run \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e CONTENTION_VUS=100 \
  load-test/k6/contention.js
```

```bash
k6 run \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e CONTENTION_VUS=100 \
  load-test/k6/contention.js
```

## 환경변수

- `BASE_URL`
- `ADMIN_NAME`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `TEST_USER_PASSWORD`
- `STARTUP_TIMEOUT_SECONDS`
- `STARTUP_POLL_INTERVAL_SECONDS`
- `SMOKE_VUS`
- `BASELINE_VUS`
- `BASELINE_DURATION`
- `BASELINE_SESSION_POOL_SIZE`
- `BASELINE_COUPON_POOL_SIZE`
- `CONTENTION_VUS`
- `CONTENTION_MAX_DURATION`
- `RESULTS_DIR`

## 결과 확인

- `load-test/k6/results/*-latest.json`
- `load-test/k6/results/*-<timestamp>.json`
- Grafana `k6 / k6 Overview`

애플리케이션 측 지표는 같은 시간대의 `/actuator/prometheus`를 같이 확인합니다.

- HTTP 지연과 오류율
- JVM 메모리/GC
- Tomcat thread 사용량
- DB/Redis 관련 기본 Micrometer 지표
사용자가 말한 “N분 동안 M명이 `coupon-issue`를 과부하게 실행”은 아래 시나리오로 바로 실행합니다.

```bash
k6 run \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_OVERLOAD_VUS=100 \
  -e ISSUE_OVERLOAD_DURATION=10m \
  -e ISSUE_OVERLOAD_USER_POOL_SIZE=200 \
  -e ISSUE_OVERLOAD_COUPON_POOL_SIZE=500 \
  load-test/k6/issue-overload.js
```

의미는 아래와 같습니다.

- `ISSUE_OVERLOAD_VUS`
  - 동시에 계속 요청을 보내는 사용자 수, 즉 M입니다.
- `ISSUE_OVERLOAD_DURATION`
  - 부하를 유지할 시간, 즉 N분입니다.
- `ISSUE_OVERLOAD_USER_POOL_SIZE`
  - 발급 대상 사용자 풀 크기입니다.
- `ISSUE_OVERLOAD_COUPON_POOL_SIZE`
  - 발급 대상 쿠폰 풀 크기입니다.

`issue-overload`의 유니크 발급 가능량은 아래입니다.

- `ISSUE_OVERLOAD_USER_POOL_SIZE x ISSUE_OVERLOAD_COUPON_POOL_SIZE`

예를 들어 `200 users x 500 coupons = 100,000`건까지는 중복 없이 `coupon-issue`를 실행할 수 있습니다. 지속 실행 중 이 용량을 넘기면 스크립트가 아래처럼 테스트를 즉시 abort 하면서 원인을 직접 알려줍니다.

- `issue_overload capacity exhausted ... Increase ISSUE_OVERLOAD_USER_POOL_SIZE or ISSUE_OVERLOAD_COUPON_POOL_SIZE`

처음에는 쿠폰을 늘리는 편이 사용자까지 늘리는 것보다 setup 비용이 덜 큽니다.

- `ISSUE_OVERLOAD_VUS`
- `ISSUE_OVERLOAD_DURATION`
- `ISSUE_OVERLOAD_USER_POOL_SIZE`
- `ISSUE_OVERLOAD_COUPON_POOL_SIZE`
- `ISSUE_OVERLOAD_SETUP_TIMEOUT`

## 실패 원인 빠르게 보는 법

- `issue_overload capacity exhausted`
  - 테스트 데이터 조합이 부족합니다.
  - `ISSUE_OVERLOAD_USER_POOL_SIZE x ISSUE_OVERLOAD_COUPON_POOL_SIZE`를 늘립니다.
- `issue_coupon: unexpected response status=409` 또는 관련 4xx
  - 중복 발급, 만료, 비활성 쿠폰처럼 비즈니스 조건이 깨진 상태입니다.
  - setup에서 만든 쿠폰 수량과 상태, 사용자/쿠폰 조합 소진 여부를 먼저 봅니다.
- `issue_coupon: unexpected response status=500`
  - 서버가 실제로 버티지 못한 것입니다.
  - 같은 시간대 Grafana의 `p95/p99`, failed rate, `/actuator/prometheus`, 애플리케이션 로그를 같이 봅니다.
- `app_ready` 또는 `admin_signin_ready` timeout
  - 부하 테스트 이전에 앱 기동이나 local bootstrap이 준비되지 않은 상태입니다.
  - `docker compose ... up --build`, `/actuator/health`, `/load-test/admin/signin`부터 다시 확인합니다.
