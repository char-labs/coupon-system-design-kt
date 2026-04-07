#!/usr/bin/env bash

# Local coupon Kafka load-test runbook.
#
# Current standard scenario:
# - "사용자 1,000명 동시 발급 시도 / 쿠폰 수량 1,000개" 를 기준으로 정합성과 동시성을 먼저 검증합니다.
# - ramp, overload, real-ramp 는 그 다음 단계의 확장 시나리오로 둡니다.
#
# Scenario intent:
# - smoke
#   - 1명의 사용자가 1개의 쿠폰 발급 요청을 보내고, request 상태가 정상적으로 terminal status로 수렴하는지 확인합니다.
#   - measured phase 전에 synthetic 사용자를 미리 준비합니다.
# - ramp
#   - synthetic user 기준 Kafka intake 성능만 봅니다.
#   - 기본값은 3,000 -> 5,000 -> 7,000 VU로 점진 증가시키고, 재고는 충분히 크게 잡아 품절이 병목으로 끼지 않게 합니다.
#   - setup에서 synthetic 사용자 풀과 쿠폰 풀을 먼저 준비하고, 본 실행에서는 발급 요청만 보냅니다.
# - real-ramp
#   - 실제 인증된 사용자 세션 풀로 intake를 확인합니다.
#   - 기본값은 사용자 풀 7,000명, 쿠폰 풀 1,000개, 쿠폰당 재고 100,000개입니다.
# - burst
#   - 한 종류의 쿠폰에 대해 동시에 몰리는 선착순 상황을 검증합니다.
#   - 기본값은 "사용자 1,000명 동시 발급 시도 / 쿠폰 수량 1,000개"입니다.
#   - setup에서 사용자 1,000명을 먼저 bulk prepare한 뒤, measured phase에서 동시에 쿠폰 발급을 호출합니다.
#   - 지금 기본 검증 대상은 이 시나리오입니다.
# - overload
#   - 일정 시간 동안 지속적으로 발급 요청이 들어오는 상황을 검증합니다.
#   - 기본값은 50 VU가 10분 동안 200개 쿠폰 풀을 대상으로 요청하고, 쿠폰당 재고는 100,000개입니다.
#   - setup에서 synthetic 사용자 풀과 쿠폰 풀을 미리 준비해, sustained load 동안 user 생성이 섞이지 않게 합니다.
#
# Read this file as:
# - up/check: 로컬 스택 준비
# - smoke: 배포 직후 최소 회귀 확인
# - burst: 표준 동시성/정합성 확인
# - ramp/real-ramp: intake 처리량 확장 확인
# - overload: 지속 부하 안정성 확인

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_URL="${BASE_URL:-http://127.0.0.1:18080}"
INFLUX_OUT="${INFLUX_OUT:-influxdb=http://localhost:8086/myk6db}"
PROFILE="${PROFILE:-local}"
ADMIN_EMAIL="${ADMIN_EMAIL:-loadtest-admin@coupon.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin1234!}"

COMPOSE_FILES=(
  "-f" "${ROOT_DIR}/docker/docker-compose.yml"
  "-f" "${ROOT_DIR}/docker/docker-compose.k6-observability.yml"
)

run_scenario() {
  local scenario="$1"
  shift || true

  (
    cd "${ROOT_DIR}"
    node load-test/k6/run-with-slack.mjs "${scenario}" --profile "${PROFILE}" -- \
      --out "${INFLUX_OUT}" \
      -e BASE_URL="${BASE_URL}" \
      -e ADMIN_EMAIL="${ADMIN_EMAIL}" \
      -e ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
      "$@"
  )
}

start_stack() {
  (
    cd "${ROOT_DIR}"
    docker compose "${COMPOSE_FILES[@]}" up --build -d
  )
}

wait_for_http() {
  local name="$1"
  local url="$2"

  echo "[wait] ${name}: ${url}"
  for _ in $(seq 1 60); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "[ok] ${name}"
      return 0
    fi

    sleep 2
  done

  echo "[fail] ${name} did not become ready: ${url}" >&2
  return 1
}

check_stack() {
  wait_for_http "coupon-app health" "${BASE_URL}/actuator/health"
  wait_for_http "coupon-app ping" "${BASE_URL}/ping"
  wait_for_http "coupon-worker health" "http://127.0.0.1:18081/actuator/health"
  wait_for_http "influxdb" "http://127.0.0.1:8086/ping"
  wait_for_http "grafana" "http://127.0.0.1:3000/api/health"
  wait_for_http "kafka-ui" "http://127.0.0.1:18085"
}

usage() {
  cat <<'EOF'
Usage:
  ./load-test/k6/run-local-kafka-runbook.sh up
  ./load-test/k6/run-local-kafka-runbook.sh check
  ./load-test/k6/run-local-kafka-runbook.sh smoke
  ./load-test/k6/run-local-kafka-runbook.sh ramp
  ./load-test/k6/run-local-kafka-runbook.sh real-ramp
  ./load-test/k6/run-local-kafka-runbook.sh burst
  ./load-test/k6/run-local-kafka-runbook.sh overload
  ./load-test/k6/run-local-kafka-runbook.sh full

Environment overrides:
  BASE_URL=http://127.0.0.1:18080
  PROFILE=local
  INFLUX_OUT=influxdb=http://localhost:8086/myk6db
  ADMIN_EMAIL=loadtest-admin@coupon.local
  ADMIN_PASSWORD='admin1234!'

Behavior:
  up        Start docker stack only
  check     Verify app, worker, Grafana, InfluxDB, Kafka UI
  smoke     Run accepted-model smoke test
  ramp      Run synthetic Kafka intake ramp test (optional)
  real-ramp Run real-user Kafka intake ramp test
  burst     Run standard accepted-model burst convergence test
  overload  Run accepted-model sustained overload test
  full      up -> check -> smoke -> burst

Scenario examples:
  기본 burst
    사용자 1,000명 동시 발급 시도 / 쿠폰 수량 1,000개

  sustained overload
    ISSUE_REQUEST_OVERLOAD_VUS=200 ISSUE_REQUEST_OVERLOAD_DURATION=15m \
      ./load-test/k6/run-local-kafka-runbook.sh overload
EOF
}

main() {
  local mode="${1:-full}"

  case "${mode}" in
    up)
      start_stack
      ;;
    check)
      check_stack
      ;;
    smoke)
      run_scenario issue-request-smoke
      ;;
    ramp)
      run_scenario issue-request-ramp
      ;;
    real-ramp)
      run_scenario issue-request-real-ramp
      ;;
    burst)
      run_scenario issue-request-burst
      ;;
    overload)
      run_scenario issue-request-overload
      ;;
    full)
      start_stack
      check_stack
      run_scenario issue-request-smoke
      run_scenario issue-request-burst
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"
