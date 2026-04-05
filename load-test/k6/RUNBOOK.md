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
curl http://127.0.0.1:18080/ping
curl http://127.0.0.1:18080/actuator/health
curl -X POST http://127.0.0.1:18080/load-test/admin/signin \
  -H 'Content-Type: application/json' \
  -d '{}'
curl -i http://localhost:8086/ping
curl http://localhost:3000/api/health
```

`k6` 스크립트는 `setup()`에서 `/actuator/health`를 확인한 뒤, local profile 전용 `/load-test/admin/signin`으로 관리자 계정 보장과 토큰 발급을 먼저 시도합니다. 그래도 첫 실행 안정성을 위해 위 준비 확인을 먼저 하는 편이 좋습니다.

`BASE_URL`은 `localhost` 대신 `127.0.0.1`을 권장합니다. 로컬 환경에 따라 `localhost`가 IPv6 또는 다른 resolver 경로를 타면서 진단이 어려워질 수 있습니다.

기본 Docker 앱 포트는 `18080`입니다. IDE나 `bootRun`이 `8080`을 이미 쓰고 있어도 부하 테스트 대상이 Docker 앱으로 고정되게 하기 위한 기본값입니다. 다른 포트를 쓰고 싶으면 `APP_HOST_PORT`로 덮어쓸 수 있습니다.

기본 접속 정보:

- 앱: `http://localhost:18080`
- 앱 권장 BASE_URL: `http://127.0.0.1:18080`
- InfluxDB: `http://localhost:8086`
- Grafana: `http://localhost:3000`
- Grafana 계정: `admin / admin`

`ADMIN_PASSWORD`처럼 `!`가 포함된 값은 셸 해석 이슈를 피하려고 따옴표로 감싸는 편이 안전합니다.

Slack 실패 알림을 같이 쓰려면 [load-test/k6/.env](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/.env)에 `LOAD_TEST_SLACK_WEBHOOK=` 값을 넣고, plain `k6 run` 대신 [run-with-slack.mjs](/Users/yunbeom/ybcha/coupon-system-design-kt/load-test/k6/run-with-slack.mjs)로 실행합니다.
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
  -e ISSUE_BURST_STOCK=1000
```

재고보다 요청이 많을 때도 서버가 터지지 않고 제어되는지 보는 케이스:

```bash
node load-test/k6/run-with-slack.mjs issue-burst --profile local -- \
  --out influxdb=http://localhost:8086/myk6db \
  -e BASE_URL=http://127.0.0.1:18080 \
  -e ISSUE_BURST_VUS=1000 \
  -e ISSUE_BURST_STOCK=900
```

이 시나리오는 실행 뒤 아래를 자동으로 다시 확인합니다.

- 최종 발급 건수
- 최종 잔여 재고
- `발급 건수 + 잔여 재고 == 초기 재고` 정합성
- `min(VU, 재고)`와 실제 결과가 일치하는지
- 서버 오류가 없었는지

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

중요:

- 실시간 대시보드를 보려면 반드시 `--out influxdb=http://localhost:8086/myk6db`를 붙입니다.
- `--out` 없이 실행하면 JSON summary만 남고 Grafana에는 안 보입니다.
- `run-with-slack.mjs`는 `load-test/k6/.env`를 자동으로 읽고, 실패 시 Slack webhook으로 메세지를 보냅니다.
- 앱이 막 기동된 상태라면 `setup()`이 health와 admin signin을 몇 초간 대기할 수 있습니다.

## 3-1. Slack 실패 알림 확인

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

원인 보는 순서:

1. Slack 또는 결과 JSON에서 `issue_burst_server_error_count`, `issue_burst_unexpected_client_error_count`를 먼저 확인
2. `issue_burst_final_issued_count`, `issue_burst_final_remaining_quantity`를 보고 재고 합계가 맞는지 확인
3. Grafana에서 같은 시간대 `p95`, `p99`, `HTTP Request Failed Rate`를 확인
4. 동시에 앱 로그와 `/actuator/prometheus`에서 DB 커넥션 풀, 스레드, 에러 수치를 확인

빠른 판정:

- `정합성 통과 + 서버 오류 0`
  - 재고 동시성 제어는 정상으로 판단 가능
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
