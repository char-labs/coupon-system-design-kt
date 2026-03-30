# k6 Dashboard Runbook

이 문서는 `k6 -> InfluxDB -> Grafana` 흐름으로 테스트를 실행하고, 대시보드를 보면서 결과를 해석하는 최소 운영 가이드입니다.

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
curl http://127.0.0.1:8080/ping
curl http://127.0.0.1:8080/actuator/health
curl -X POST http://127.0.0.1:8080/signin \
  -H 'Content-Type: application/json' \
  -d '{"email":"loadtest-admin@coupon.local","password":"admin1234!"}'
curl -i http://localhost:8086/ping
curl http://localhost:3000/api/health
```

`k6` 스크립트는 `setup()`에서 `/actuator/health`와 관리자 로그인 성공을 최대 60초까지 재시도합니다. 그래도 첫 실행 안정성을 위해 위 준비 확인을 먼저 하는 편이 좋습니다.

`BASE_URL`은 `localhost` 대신 `127.0.0.1`을 권장합니다. 로컬 환경에 따라 `localhost`가 IPv6 또는 다른 resolver 경로를 타면서 진단이 어려워질 수 있습니다.

기본 접속 정보:

- 앱: `http://localhost:8080`
- 앱 권장 BASE_URL: `http://127.0.0.1:8080`
- InfluxDB: `http://localhost:8086`
- Grafana: `http://localhost:3000`
- Grafana 계정: `admin / admin`

`ADMIN_PASSWORD`처럼 `!`가 포함된 값은 셸 해석 이슈를 피하려고 따옴표로 감싸는 편이 안전합니다.

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
k6 run \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:8080 \
  -e ADMIN_EMAIL=loadtest-admin@coupon.local \
  -e ADMIN_PASSWORD='admin1234!' \
  load-test/k6/smoke.js
```

### Baseline

```bash
k6 run \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:8080 \
  -e BASELINE_VUS=20 \
  -e BASELINE_DURATION=10m \
  load-test/k6/baseline.js
```

### Contention

```bash
k6 run \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:8080 \
  -e CONTENTION_VUS=100 \
  load-test/k6/contention.js
```

중요:

- 실시간 대시보드를 보려면 반드시 `--out influxdb=http://localhost:8086/myk6db`를 붙입니다.
- `--out` 없이 실행하면 JSON summary만 남고 Grafana에는 안 보입니다.
- 앱이 막 기동된 상태라면 `setup()`이 health와 admin signin을 몇 초간 대기할 수 있습니다.

## 4. 실행 중 확인 포인트

`k6 Overview`는 아래 5개 패널을 기준으로 봅니다.

- `HTTP Request Duration p95`
  - tail latency를 봅니다.
  - 완만하게 유지되면 정상, 계단식 상승이면 포화 구간 가능성이 큽니다.
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

### Baseline

목표:

- steady-state에서 성능 기준선 확보
- 장시간 실행 중 tail latency와 처리량 안정성 확인

정상 신호:

- p95가 큰 흔들림 없이 유지
- failed rate가 장시간 0 근처
- iterations가 시간축에서 비교적 일정
- VU는 고정, iterations도 유지

이상 신호:

- 시간이 갈수록 p95가 계속 상승
- failed rate가 후반부로 갈수록 증가
- VU는 유지되는데 iterations가 눌림

이 경우에는 같은 시간대의 아래를 같이 봅니다.

- `/actuator/prometheus`
- 애플리케이션 로그
- MySQL/Redis 컨테이너 상태

### Contention

목표:

- 동일 쿠폰 동시 발급 시 정합성과 병목 구간 확인

정상 신호:

- p95 상승은 있어도 checks rate는 크게 깨지지 않음
- iterations가 빠르게 몰리면서도 실패율이 통제됨

이상 신호:

- p95 급등과 함께 failed rate가 같이 오름
- checks rate가 떨어짐
- iterations가 급격히 눌리거나 plateau가 생김

주의:

- contention은 baseline보다 latency spike가 더 자연스럽습니다.
- 중요한 건 spike 자체보다, 그 spike가 지속되는지와 실패율이 함께 올라가는지입니다.

## 6. 앱 메트릭과 함께 보기

현재 Grafana에는 `k6` 메트릭만 들어갑니다. 앱 메트릭은 별도로 확인합니다.

추천 흐름:

1. Grafana에서 `k6` 패널로 부하 구간 확인
2. 같은 시간 범위에 맞춰 `/actuator/prometheus` 또는 로그 확인
3. 아래 관점으로 연결해서 해석

- p95 상승 시점과 Tomcat thread/connection 포화 시점이 겹치는지
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

1. `curl http://127.0.0.1:8080/ping`
2. `curl http://127.0.0.1:8080/actuator/health`
3. `curl -X POST http://127.0.0.1:8080/signin -H 'Content-Type: application/json' -d '{"email":"loadtest-admin@coupon.local","password":"admin1234!"}'`
4. `k6` 실행 시 `-e BASE_URL=http://127.0.0.1:8080`를 썼는지
5. `ADMIN_PASSWORD`를 `'admin1234!'`처럼 따옴표로 감쌌는지
6. admin 계정이 local bootstrap으로 생성됐는지
7. 기존 MySQL volume 때문에 `coupon-admin` 계정이 꼬이지 않았는지

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
