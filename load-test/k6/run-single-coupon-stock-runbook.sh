#!/usr/bin/env bash

# Single-coupon stock-focused load-test runbook.
#
# Purpose:
# - Keep the target fixed to "쿠폰 1개"로 두고, 재고 수량만 바꿔가며 동시 발급 정합성을 검증합니다.
# - 모든 시나리오는 issue-request-burst를 재사용하며, measured phase에서는 사용자 1,000명이 동시에 같은 쿠폰 발급을 시도합니다.
#
# Standard cases:
# - exact
#   - 사용자 1,000명 / 쿠폰 수량 1,000개
#   - 기대: 1,000건 성공, 잔여 재고 0, DEAD 0
# - oversubscribed
#   - 사용자 1,000명 / 쿠폰 수량 900개
#   - 기대: 900건 성공, 100건 OUT_OF_STOCK, 잔여 재고 0
# - scarce
#   - 사용자 1,000명 / 쿠폰 수량 100개
#   - 기대: 100건 성공, 900건 OUT_OF_STOCK, 잔여 재고 0
# - single-stock
#   - 사용자 1,000명 / 쿠폰 수량 1개
#   - 기대: 1건 성공, 999건 OUT_OF_STOCK, 잔여 재고 0
#
# Read this file as:
# - up/check: 로컬 스택 준비
# - exact: 표준 1,000명 동시성 검증
# - oversubscribed/scarce/single-stock: 재고 소진/제어 동작 검증

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_URL="${BASE_URL:-http://127.0.0.1:18080}"
INFLUX_OUT="${INFLUX_OUT:-influxdb=http://localhost:8086/myk6db}"
PROFILE="${PROFILE:-local}"
ADMIN_EMAIL="${ADMIN_EMAIL:-loadtest-admin@coupon.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin1234!}"
BURST_VUS="${BURST_VUS:-1000}"
EXACT_STOCK="${EXACT_STOCK:-1000}"
OVERSUBSCRIBED_STOCK="${OVERSUBSCRIBED_STOCK:-900}"
SCARCE_STOCK="${SCARCE_STOCK:-100}"
SINGLE_STOCK="${SINGLE_STOCK:-1}"
POLL_TIMEOUT_SECONDS="${POLL_TIMEOUT_SECONDS:-30}"
POLL_INTERVAL_MS="${POLL_INTERVAL_MS:-500}"

COMPOSE_FILES=(
  "-f" "${ROOT_DIR}/docker/docker-compose.yml"
  "-f" "${ROOT_DIR}/docker/docker-compose.k6-observability.yml"
)

run_burst_case() {
  local stock="$1"

  (
    cd "${ROOT_DIR}"
    node load-test/k6/run-with-slack.mjs issue-request-burst --profile "${PROFILE}" -- \
      --out "${INFLUX_OUT}" \
      -e BASE_URL="${BASE_URL}" \
      -e ADMIN_EMAIL="${ADMIN_EMAIL}" \
      -e ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
      -e ISSUE_REQUEST_BURST_VUS="${BURST_VUS}" \
      -e ISSUE_REQUEST_BURST_STOCK="${stock}" \
      -e ISSUE_REQUEST_POLL_TIMEOUT_SECONDS="${POLL_TIMEOUT_SECONDS}" \
      -e ISSUE_REQUEST_POLL_INTERVAL_MS="${POLL_INTERVAL_MS}"
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
  ./load-test/k6/run-single-coupon-stock-runbook.sh up
  ./load-test/k6/run-single-coupon-stock-runbook.sh check
  ./load-test/k6/run-single-coupon-stock-runbook.sh exact
  ./load-test/k6/run-single-coupon-stock-runbook.sh oversubscribed
  ./load-test/k6/run-single-coupon-stock-runbook.sh scarce
  ./load-test/k6/run-single-coupon-stock-runbook.sh single-stock
  ./load-test/k6/run-single-coupon-stock-runbook.sh full

Environment overrides:
  BASE_URL=http://127.0.0.1:18080
  PROFILE=local
  INFLUX_OUT=influxdb=http://localhost:8086/myk6db
  ADMIN_EMAIL=loadtest-admin@coupon.local
  ADMIN_PASSWORD='admin1234!'
  BURST_VUS=1000
  EXACT_STOCK=1000
  OVERSUBSCRIBED_STOCK=900
  SCARCE_STOCK=100
  SINGLE_STOCK=1
  POLL_TIMEOUT_SECONDS=30
  POLL_INTERVAL_MS=500

Behavior:
  up              Start docker stack only
  check           Verify app, worker, Grafana, InfluxDB, Kafka UI
  exact           1,000 users / 1 coupon / stock 1,000
  oversubscribed  1,000 users / 1 coupon / stock 900
  scarce          1,000 users / 1 coupon / stock 100
  single-stock    1,000 users / 1 coupon / stock 1
  full            up -> check -> exact -> oversubscribed -> single-stock
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
    exact)
      run_burst_case "${EXACT_STOCK}"
      ;;
    oversubscribed)
      run_burst_case "${OVERSUBSCRIBED_STOCK}"
      ;;
    scarce)
      run_burst_case "${SCARCE_STOCK}"
      ;;
    single-stock)
      run_burst_case "${SINGLE_STOCK}"
      ;;
    full)
      start_stack
      check_stack
      run_burst_case "${EXACT_STOCK}"
      run_burst_case "${OVERSUBSCRIBED_STOCK}"
      run_burst_case "${SINGLE_STOCK}"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"
