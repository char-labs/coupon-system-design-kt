#!/usr/bin/env node

import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises';
import http from 'node:http';
import https from 'node:https';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDotEnvPath = path.resolve(__dirname, '..', '..', '.env');
const legacyDotEnvPath = path.resolve(__dirname, '.env');
const SLOW_REQUEST_SAMPLE_MARKER = '__K6_SLOW_REQUEST_SAMPLE__';

const scenarioDefaults = {
  smoke: {
    SMOKE_VUS: '1',
  },
  baseline: {
    BASELINE_VUS: '20',
    BASELINE_DURATION: '10m',
  },
  'issue-burst': {
    ISSUE_BURST_VUS: '1000',
    ISSUE_BURST_STOCK: '1000',
    ISSUE_BURST_MAX_DURATION: '5m',
  },
  'restaurant-issue-burst': {
    ISSUE_BURST_VUS: '1000',
    ISSUE_BURST_STOCK: '1000',
    ISSUE_BURST_MAX_DURATION: '5m',
  },
  contention: {
    CONTENTION_VUS: '100',
    CONTENTION_MAX_DURATION: '2m',
  },
  'issue-overload': {
    ISSUE_OVERLOAD_VUS: '100',
    ISSUE_OVERLOAD_DURATION: '10m',
    ISSUE_OVERLOAD_USER_POOL_SIZE: '200',
    ISSUE_OVERLOAD_COUPON_POOL_SIZE: '500',
  },
  'issue-ramp': {
    ISSUE_RAMP_STOCK: '10000000',
    ISSUE_RAMP_STAGE1_DURATION: '3m',
    ISSUE_RAMP_STAGE1_TARGET: '3000',
    ISSUE_RAMP_STAGE2_DURATION: '1m',
    ISSUE_RAMP_STAGE2_TARGET: '3000',
    ISSUE_RAMP_STAGE3_DURATION: '2m',
    ISSUE_RAMP_STAGE3_TARGET: '5000',
    ISSUE_RAMP_STAGE4_DURATION: '3m',
    ISSUE_RAMP_STAGE4_TARGET: '5000',
    ISSUE_RAMP_STAGE5_DURATION: '2m',
    ISSUE_RAMP_STAGE5_TARGET: '7000',
    ISSUE_RAMP_STAGE6_DURATION: '5m',
    ISSUE_RAMP_STAGE6_TARGET: '7000',
    ISSUE_RAMP_STAGE7_DURATION: '3m',
    ISSUE_RAMP_STAGE7_TARGET: '0',
  },
  'issue-real-ramp': {
    ISSUE_REAL_RAMP_USER_POOL_SIZE: '7000',
    ISSUE_REAL_RAMP_COUPON_POOL_SIZE: '1000',
    ISSUE_REAL_RAMP_STOCK_PER_COUPON: '100000',
    ISSUE_REAL_RAMP_SETUP_TIMEOUT: '30m',
    ISSUE_RAMP_STAGE1_DURATION: '3m',
    ISSUE_RAMP_STAGE1_TARGET: '3000',
    ISSUE_RAMP_STAGE2_DURATION: '1m',
    ISSUE_RAMP_STAGE2_TARGET: '3000',
    ISSUE_RAMP_STAGE3_DURATION: '2m',
    ISSUE_RAMP_STAGE3_TARGET: '5000',
    ISSUE_RAMP_STAGE4_DURATION: '5m',
    ISSUE_RAMP_STAGE4_TARGET: '5000',
    ISSUE_RAMP_STAGE5_DURATION: '2m',
    ISSUE_RAMP_STAGE5_TARGET: '7000',
    ISSUE_RAMP_STAGE6_DURATION: '5m',
    ISSUE_RAMP_STAGE6_TARGET: '7000',
    ISSUE_RAMP_STAGE7_DURATION: '3m',
    ISSUE_RAMP_STAGE7_TARGET: '0',
  },
};

const scenarioLabels = {
  smoke: '기본 기능 확인',
  baseline: '일반 사용량 기준 부하',
  'issue-burst': '대량 동시 발급 정합성 확인',
  'restaurant-issue-burst': '맛집 쿠폰 동시 발급 정합성 확인',
  contention: '동시 발급 경합 확인',
  'issue-overload': '쿠폰 발급 진입 과부하',
  'issue-ramp': 'prepared-user immediate issue 성능 확인',
  'issue-real-ramp': '실사용자 immediate issue 성능 확인',
};

function usage() {
  return [
    'Usage:',
    '  node load-test/k6/run-with-slack.mjs <scenario> [--profile <name>] [--notify-on <failure|always|never>] [--webhook-url <url>] [--] [k6 run args...]',
    '',
    'Example:',
    '  node load-test/k6/run-with-slack.mjs issue-burst --profile local -- \\',
    '    --out influxdb=http://localhost:8086/myk6db \\',
    '    -e BASE_URL=http://127.0.0.1:18080 \\',
    '    -e ISSUE_BURST_VUS=1000 \\',
    '    -e ISSUE_BURST_STOCK=1000',
  ].join('\n');
}

async function loadDotEnv() {
  const env = {};

  for (const envPath of [legacyDotEnvPath, rootDotEnvPath]) {
    try {
      const raw = await readFile(envPath, 'utf8');

      for (const line of raw.split(/\r?\n/)) {
        const trimmed = line.trim();

        if (!trimmed || trimmed.startsWith('#')) {
          continue;
        }

        const separatorIndex = trimmed.indexOf('=');
        if (separatorIndex <= 0) {
          continue;
        }

        const key = trimmed.slice(0, separatorIndex).trim();
        const value = trimmed.slice(separatorIndex + 1).trim().replace(/^['"]|['"]$/g, '');
        env[key] = value;
      }
    } catch (error) {
      // Missing local env files are optional.
    }
  }

  return env;
}

function parseArgs(argv, envSource) {
  const parsed = {
    scenario: null,
    profile: envSource.LOAD_TEST_PROFILE || envSource.SPRING_PROFILES_ACTIVE || 'local',
    notifyOn: envSource.LOAD_TEST_SLACK_NOTIFY_ON || envSource.SLACK_NOTIFY_ON || 'failure',
    webhookUrl: envSource.LOAD_TEST_SLACK_WEBHOOK || envSource.SLACK_WEBHOOK_URL || '',
    k6Args: [],
  };

  let forwardArgs = false;

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];

    if (forwardArgs) {
      parsed.k6Args.push(arg);
      continue;
    }

    if (arg === '--') {
      forwardArgs = true;
      continue;
    }

    if (!parsed.scenario && !arg.startsWith('-')) {
      parsed.scenario = arg;
      continue;
    }

    if (arg === '--profile') {
      parsed.profile = argv[index + 1];
      index += 1;
      continue;
    }

    if (arg.startsWith('--profile=')) {
      parsed.profile = arg.split('=', 2)[1];
      continue;
    }

    if (arg === '--notify-on') {
      parsed.notifyOn = argv[index + 1];
      index += 1;
      continue;
    }

    if (arg.startsWith('--notify-on=')) {
      parsed.notifyOn = arg.split('=', 2)[1];
      continue;
    }

    if (arg === '--webhook-url') {
      parsed.webhookUrl = argv[index + 1];
      index += 1;
      continue;
    }

    if (arg.startsWith('--webhook-url=')) {
      parsed.webhookUrl = arg.split('=', 2)[1];
      continue;
    }

    throw new Error(`Unsupported argument: ${arg}\n\n${usage()}`);
  }

  if (!parsed.scenario) {
    throw new Error(`Scenario is required.\n\n${usage()}`);
  }

  if (!['failure', 'always', 'never'].includes(parsed.notifyOn)) {
    throw new Error(`Unsupported --notify-on value: ${parsed.notifyOn}`);
  }

  return parsed;
}

function parseK6EnvArgs(args) {
  const env = {};

  const readEntry = (entry) => {
    const separatorIndex = entry.indexOf('=');
    if (separatorIndex <= 0) {
      return;
    }

    const key = entry.slice(0, separatorIndex);
    const value = entry.slice(separatorIndex + 1);
    env[key] = value;
  };

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index];

    if ((arg === '-e' || arg === '--env') && args[index + 1]) {
      readEntry(args[index + 1]);
      index += 1;
      continue;
    }

    if (arg.startsWith('--env=')) {
      readEntry(arg.slice('--env='.length));
      continue;
    }

    if (arg.startsWith('-e') && arg.length > 2) {
      readEntry(arg.slice(2));
    }
  }

  return env;
}

function metricValue(summary, key, nestedKey) {
  const metric = summary?.metrics?.[key];
  if (!metric) {
    return 'n/a';
  }

  if (nestedKey) {
    return metric.values?.[nestedKey] ?? 'n/a';
  }

  return metric.values?.value ?? 'n/a';
}

function formatDuration(value) {
  if (value === 'n/a') {
    return '측정 안 됨';
  }

  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return String(value);
  }

  return `${numericValue.toFixed(2)}ms`;
}

function formatRate(value) {
  if (value === 'n/a') {
    return '측정 안 됨';
  }

  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return String(value);
  }

  return `${(numericValue * 100).toFixed(2)}%`;
}

function collectThresholdFailures(summary) {
  const failures = [];

  for (const [metricName, metric] of Object.entries(summary?.metrics || {})) {
    for (const [thresholdName, threshold] of Object.entries(metric.thresholds || {})) {
      if (threshold.ok === false) {
        failures.push(`${metricName} ${thresholdName}`);
      }
    }
  }

  return failures;
}

function cleanErrorText(text) {
  return text
    .replace(/\\n\\tat[\s\S]*/g, '')
    .replace(/\s+at\s+go\.k6[\s\S]*/g, '')
    .replace(/\s+at\s+\S+\s+\(file:.*$/g, '')
    .trim();
}

function extractFailureReason(lines) {
  for (let index = lines.length - 1; index >= 0; index -= 1) {
    const line = lines[index];

    if (!line) {
      continue;
    }

    const goErrorMatch = line.match(/GoError:\s*(.+)$/);
    if (goErrorMatch) {
      return cleanErrorText(goErrorMatch[1]);
    }

    const levelErrorMatch = line.match(/level=error msg="([^"]+)"/);
    if (levelErrorMatch) {
      return cleanErrorText(levelErrorMatch[1]);
    }

    const erroMatch = line.match(/ERRO\[[^\]]+\]\s*(.+)$/);
    if (erroMatch) {
      return cleanErrorText(erroMatch[1]);
    }
  }

  return null;
}

function resolveScenarioScript(scenario) {
  const scriptPath = path.resolve(__dirname, `${scenario}.js`);

  if (!existsSync(scriptPath)) {
    throw new Error(`Scenario script not found: ${scriptPath}`);
  }

  return scriptPath;
}

function resolveResultsDir(parsedEnv, envSource) {
  const resultsDir = parsedEnv.RESULTS_DIR || envSource.RESULTS_DIR || 'load-test/k6/results';
  return path.resolve(process.cwd(), resultsDir);
}

function resolveSlowRequestSampleLimit(parsedEnv, envSource) {
  const value = Number(
    parsedEnv.SLOW_REQUEST_SAMPLE_LIMIT || envSource.SLOW_REQUEST_SAMPLE_LIMIT || 10,
  );

  return Number.isFinite(value) && value > 0 ? value : 10;
}

function parseSlowRequestSampleLine(line) {
  const parsePayload = (payload) => {
    try {
      return JSON.parse(payload);
    } catch (error) {
      try {
        return JSON.parse(decodeURIComponent(payload));
      } catch (decodeError) {
        return null;
      }
    }
  };

  if (line.startsWith(SLOW_REQUEST_SAMPLE_MARKER)) {
    return parsePayload(line.slice(SLOW_REQUEST_SAMPLE_MARKER.length));
  }

  const markerIndex = line.indexOf(SLOW_REQUEST_SAMPLE_MARKER);
  if (markerIndex < 0) {
    return null;
  }

  const rawPayload = line.slice(markerIndex + SLOW_REQUEST_SAMPLE_MARKER.length);
  const consoleSuffixIndex = rawPayload.lastIndexOf('" source=console');
  if (consoleSuffixIndex >= 0) {
    return parsePayload(rawPayload.slice(0, consoleSuffixIndex));
  }

  return parsePayload(rawPayload);
}

function createOutputProcessor(writer, recentLines, slowRequestSamples) {
  let buffer = '';

  const remember = (line) => {
    if (!line) {
      return;
    }

    recentLines.push(line);
    if (recentLines.length > 300) {
      recentLines.shift();
    }
  };

  const handleLine = (line) => {
    const sample = parseSlowRequestSampleLine(line);
    if (sample) {
      slowRequestSamples.push(sample);
      return;
    }

    writer(`${line}\n`);
    remember(line);
  };

  return {
    push(chunk) {
      buffer += chunk.toString('utf8');
      const lines = buffer.split(/\r?\n/);
      buffer = lines.pop() ?? '';

      for (const line of lines) {
        handleLine(line);
      }
    },
    flush() {
      if (!buffer) {
        return;
      }

      handleLine(buffer);
      buffer = '';
    },
  };
}

function buildSlowRequestSummary({
  scenario,
  finishedAt,
  sampleLimit,
  slowRequestSamples,
}) {
  const sortedSamples = [...slowRequestSamples]
    .sort((left, right) => Number(right.durationMs || 0) - Number(left.durationMs || 0))
    .slice(0, sampleLimit);

  return {
    scenario,
    generatedAt: finishedAt.toISOString(),
    sampleLimit,
    totalCollectedSamples: slowRequestSamples.length,
    topSlowRequests: sortedSamples,
  };
}

async function writeSlowRequestSummary(resultsDir, scenario, finishedAt, sampleLimit, slowRequestSamples) {
  const payload = buildSlowRequestSummary({
    scenario,
    finishedAt,
    sampleLimit,
    slowRequestSamples,
  });
  const timestamp = finishedAt.toISOString().replace(/[:.]/g, '-');
  const latestPath = path.resolve(resultsDir, `${scenario}-slow-requests-latest.json`);
  const timestampPath = path.resolve(resultsDir, `${scenario}-slow-requests-${timestamp}.json`);
  const serialized = JSON.stringify(payload, null, 2);

  await writeFile(latestPath, serialized);
  await writeFile(timestampPath, serialized);
}

function buildLoadDescription(scenario, parsedEnv, envSource) {
  const defaults = scenarioDefaults[scenario] || {};
  const resolveValue = (key) => parsedEnv[key] || envSource[key] || defaults[key] || 'n/a';

  switch (scenario) {
    case 'smoke':
      return `SMOKE_VUS=${resolveValue('SMOKE_VUS')}`;
    case 'baseline':
      return `BASELINE_VUS=${resolveValue('BASELINE_VUS')}, BASELINE_DURATION=${resolveValue('BASELINE_DURATION')}`;
    case 'issue-burst':
    case 'restaurant-issue-burst':
      return [
        `ISSUE_BURST_VUS=${resolveValue('ISSUE_BURST_VUS')}`,
        `ISSUE_BURST_STOCK=${resolveValue('ISSUE_BURST_STOCK')}`,
        `ISSUE_BURST_MAX_DURATION=${resolveValue('ISSUE_BURST_MAX_DURATION')}`,
        `ISSUE_BURST_LOCK_RETRY_COUNT=${resolveValue('ISSUE_BURST_LOCK_RETRY_COUNT')}`,
        `ISSUE_BURST_LOCK_RETRY_DELAY_MS=${resolveValue('ISSUE_BURST_LOCK_RETRY_DELAY_MS')}`,
      ].join(', ');
    case 'contention':
      return `CONTENTION_VUS=${resolveValue('CONTENTION_VUS')}, CONTENTION_MAX_DURATION=${resolveValue('CONTENTION_MAX_DURATION')}`;
    case 'issue-overload':
      return [
        `ISSUE_OVERLOAD_VUS=${resolveValue('ISSUE_OVERLOAD_VUS')}`,
        `ISSUE_OVERLOAD_DURATION=${resolveValue('ISSUE_OVERLOAD_DURATION')}`,
        `ISSUE_OVERLOAD_USER_POOL_SIZE=${resolveValue('ISSUE_OVERLOAD_USER_POOL_SIZE')}`,
        `ISSUE_OVERLOAD_COUPON_POOL_SIZE=${resolveValue('ISSUE_OVERLOAD_COUPON_POOL_SIZE')}`,
      ].join(', ');
    case 'issue-ramp':
      return [
        `ISSUE_RAMP_STAGE1_TARGET=${resolveValue('ISSUE_RAMP_STAGE1_TARGET')}`,
        `ISSUE_RAMP_STAGE3_TARGET=${resolveValue('ISSUE_RAMP_STAGE3_TARGET')}`,
        `ISSUE_RAMP_STAGE5_TARGET=${resolveValue('ISSUE_RAMP_STAGE5_TARGET')}`,
        `ISSUE_RAMP_STOCK=${resolveValue('ISSUE_RAMP_STOCK')}`,
      ].join(', ');
    case 'issue-real-ramp':
      return [
        `ISSUE_REAL_RAMP_USER_POOL_SIZE=${resolveValue('ISSUE_REAL_RAMP_USER_POOL_SIZE')}`,
        `ISSUE_REAL_RAMP_COUPON_POOL_SIZE=${resolveValue('ISSUE_REAL_RAMP_COUPON_POOL_SIZE')}`,
        `ISSUE_REAL_RAMP_STOCK_PER_COUPON=${resolveValue('ISSUE_REAL_RAMP_STOCK_PER_COUPON')}`,
        `ISSUE_RAMP_STAGE5_TARGET=${resolveValue('ISSUE_RAMP_STAGE5_TARGET')}`,
      ].join(', ');
    default:
      return 'n/a';
  }
}

function buildLoadSummary(scenario, parsedEnv, envSource) {
  const defaults = scenarioDefaults[scenario] || {};
  const resolveValue = (key) => parsedEnv[key] || envSource[key] || defaults[key] || 'n/a';

  switch (scenario) {
    case 'smoke':
      return `${resolveValue('SMOKE_VUS')}명이 기본 기능 흐름을 1회 실행`;
    case 'baseline':
      return `${resolveValue('BASELINE_VUS')}명이 ${resolveValue('BASELINE_DURATION')} 동안 일반 사용 패턴으로 실행`;
    case 'issue-burst':
      return `${resolveValue('ISSUE_BURST_VUS')}명이 같은 쿠폰에 동시에 1회 발급 요청`;
    case 'restaurant-issue-burst':
      return `${resolveValue('ISSUE_BURST_VUS')}명이 같은 식당 쿠폰에 동시에 1회 발급 요청`;
    case 'contention':
      return `${resolveValue('CONTENTION_VUS')}명이 같은 쿠폰에 동시에 발급 요청`;
    case 'issue-overload':
      return `${resolveValue('ISSUE_OVERLOAD_VUS')}명이 ${resolveValue('ISSUE_OVERLOAD_DURATION')} 동안 immediate SUCCESS issue를 반복 호출`;
    case 'issue-ramp':
      return `prepared-user immediate issue intake를 3000 -> 5000 -> 7000 VU 단계로 상승`;
    case 'issue-real-ramp':
      return `실사용자 세션 기준으로 immediate issue intake를 3000 -> 5000 -> 7000 VU 단계로 상승`;
    default:
      return '설정값 확인 필요';
  }
}

function formatThresholdFailure(threshold) {
  if (threshold.startsWith('http_req_failed')) {
    return '오류율 기준 미달';
  }

  if (threshold.startsWith('checks')) {
    return '정상 응답 확인율 기준 미달';
  }

  if (threshold.startsWith('issue_burst_integrity_ok')) {
    return '재고 정합성 검증 실패';
  }

  if (threshold.startsWith('restaurant_issue_burst_integrity_ok')) {
    return '맛집 쿠폰 재고 정합성 검증 실패';
  }

  if (threshold.startsWith('issue_burst_expected_result_ok')) {
    return '예상 발급 수량 검증 실패';
  }

  if (threshold.startsWith('restaurant_issue_burst_expected_result_ok')) {
    return '맛집 쿠폰 예상 발급 수량 검증 실패';
  }

  if (threshold.startsWith('issue_burst_server_error_count')) {
    return '서버 오류 발생';
  }

  if (threshold.startsWith('issue_burst_transport_error_count')) {
    return '전송 계층 오류 발생';
  }

  if (threshold.startsWith('restaurant_issue_burst_server_error_count')) {
    return '맛집 쿠폰 서버 오류 발생';
  }

  if (threshold.startsWith('restaurant_issue_burst_transport_error_count')) {
    return '맛집 쿠폰 전송 계층 오류 발생';
  }

  if (threshold.startsWith('issue_burst_unexpected_client_error_count')) {
    return '예상하지 못한 응답 오류 발생';
  }

  if (threshold.startsWith('restaurant_issue_burst_unexpected_client_error_count')) {
    return '맛집 쿠폰 예상하지 못한 응답 오류 발생';
  }

  return threshold;
}

function explainFailure({ failureReason, scenario, parsedEnv, envSource, thresholdFailures }) {
  if (failureReason?.includes('capacity exhausted')) {
    const defaults = scenarioDefaults[scenario] || {};
    const resolveValue = (key) => parsedEnv[key] || envSource[key] || defaults[key] || 'n/a';
    const userPool = resolveValue('ISSUE_OVERLOAD_USER_POOL_SIZE');
    const couponPool = resolveValue('ISSUE_OVERLOAD_COUPON_POOL_SIZE');

    return {
      summary: '테스트에 쓸 사용자 또는 쿠폰 수가 부족해서 중간에 멈췄습니다.',
      cause: `현재 준비된 테스트 데이터는 사용자 ${userPool}명, 쿠폰 ${couponPool}개 수준입니다.`,
      action: 'ISSUE_OVERLOAD_USER_POOL_SIZE 또는 ISSUE_OVERLOAD_COUPON_POOL_SIZE를 늘린 뒤 다시 실행해 주세요.',
    };
  }

  if (failureReason?.includes('admin_signin_ready') || failureReason?.includes('app_ready')) {
    return {
      summary: '애플리케이션 준비가 끝나기 전에 테스트를 시작해서 실행하지 못했습니다.',
      cause: '앱 기동 또는 테스트용 관리자 로그인 준비가 아직 끝나지 않았습니다.',
      action: '/actuator/health 와 /signin 응답이 정상인지 먼저 확인해 주세요.',
    };
  }

  if (failureReason?.includes('status=409') || failureReason?.includes('ALREADY_ISSUED_COUPON')) {
    return {
      summary: '이미 발급된 대상을 다시 요청해서 테스트가 실패했습니다.',
      cause: '사용자와 쿠폰 조합이 중복되었거나 발급 조건이 맞지 않았습니다.',
      action: '사용자 풀과 쿠폰 풀을 늘리거나, 시나리오에서 중복 발급이 생기지 않는지 확인해 주세요.',
    };
  }

  if (
    failureReason?.includes('status=429') ||
    failureReason?.includes('LOCK_ACQUISITION_FAILED')
  ) {
    return {
      summary: '락 기반 경로에서 일부 요청이 제한에 걸렸습니다.',
      cause: '이 결과는 구버전 동기 락 기반 시나리오에서만 의미가 있습니다.',
      action: '현재 direct Kafka issue 시나리오에서는 lock retry보다 서버 오류와 정합성 지표를 먼저 확인해 주세요.',
    };
  }

  if (
    failureReason?.includes('status=500') ||
    failureReason?.includes('CannotCreateTransactionException')
  ) {
    return {
      summary: '서버 내부 오류로 요청을 끝까지 처리하지 못했습니다.',
      cause: 'DB 연결, 락 대기, 스레드/커넥션 풀 포화 같은 서버 병목 가능성이 있습니다.',
      action: '같은 시간대의 애플리케이션 로그, /actuator/prometheus, DB/Redis 상태를 함께 확인해 주세요.',
    };
  }

  if (thresholdFailures.includes('issue_burst_integrity_ok rate==1')) {
    return {
      summary: '부하 테스트 후 재고 정합성 검증이 맞지 않았습니다.',
      cause: '최종 발급 건수와 남은 재고 합계가 초기 재고와 일치하지 않았습니다.',
      action: '쿠폰 상세와 쿠폰별 발급 목록 totalCount를 다시 확인하고, 서버 로그와 DB 상태를 함께 점검해 주세요.',
    };
  }

  if (thresholdFailures.includes('issue_burst_expected_result_ok rate==1')) {
    return {
      summary: '예상한 발급 결과 수량과 실제 최종 상태가 다르게 나왔습니다.',
      cause: '성공 발급 건수 또는 잔여 수량이 설정한 재고 기준과 맞지 않았습니다.',
      action: 'ISSUE_BURST_STOCK, 성공 발급 건수, 남은 재고 수치를 같이 비교해 주세요.',
    };
  }

  if (thresholdFailures.includes('restaurant_issue_burst_integrity_ok rate==1')) {
    return {
      summary: '맛집 쿠폰 부하 테스트 후 재고 정합성 검증이 맞지 않았습니다.',
      cause: '최종 발급 건수와 남은 재고 합계가 초기 재고와 일치하지 않았습니다.',
      action: 'restaurantId에 연결된 couponId와 최종 발급 건수, 남은 재고를 함께 확인해 주세요.',
    };
  }

  if (thresholdFailures.includes('restaurant_issue_burst_expected_result_ok rate==1')) {
    return {
      summary: '예상한 맛집 쿠폰 발급 수량과 실제 최종 상태가 다르게 나왔습니다.',
      cause: '성공 발급 건수 또는 잔여 수량이 설정한 재고 기준과 맞지 않았습니다.',
      action: 'ISSUE_BURST_STOCK, 성공 발급 건수, 남은 재고 수치를 같이 비교해 주세요.',
    };
  }

  if (
    thresholdFailures.includes('issue_burst_transport_error_count count==0') ||
    thresholdFailures.includes('restaurant_issue_burst_transport_error_count count==0')
  ) {
    return {
      summary: '일부 요청이 응답을 받기 전에 연결이 끊기거나 시간 초과로 끝났습니다.',
      cause: 'EOF, connection reset, request timeout 같은 전송 계층 오류가 발생했습니다.',
      action: 'app 리소스, 프록시 timeout, hot path 로그량, Redis/Kafka 초기화 비용을 함께 확인해 주세요.',
    };
  }

  if (thresholdFailures.length > 0) {
    return {
      summary: '테스트는 실행됐지만, 미리 정한 성능 기준을 만족하지 못했습니다.',
      cause: '오류율 또는 정상 응답 확인율이 기준 아래로 내려갔습니다.',
      action: '기준 미달 항목과 p95/p99 지표가 언제 나빠졌는지 대시보드에서 확인해 주세요.',
    };
  }

  return {
    summary: '예상하지 못한 이유로 테스트가 실패했습니다.',
    cause: failureReason || 'k6 실행 중 오류가 발생했습니다.',
    action: '최근 에러 로그와 결과 파일을 먼저 확인해 주세요.',
  };
}

function explainSuccess({ scenario }) {
  switch (scenario) {
    case 'smoke':
      return {
        summary: '기본 기능 흐름을 오류 없이 끝까지 확인했습니다.',
        detail: '관리자 로그인, 쿠폰 생성, 사용자 로그인, 쿠폰 발급과 사용까지 정상 응답으로 완료됐습니다.',
        action: '기능 회귀 확인용 기준 결과로 저장하고, 필요하면 baseline 또는 issue-overload로 다음 단계 테스트를 진행해 주세요.',
      };
    case 'baseline':
      return {
        summary: '일반 사용량 기준 부하를 안정적으로 처리했습니다.',
        detail: '테스트 중 오류율과 정상 응답 확인율이 기준 안에 들었고, 시나리오를 끝까지 수행했습니다.',
        action: '이번 결과를 기준선으로 기록하고, 이전 실행 대비 p95/p99 변화가 없는지 비교해 주세요.',
      };
    case 'issue-burst':
      return {
        summary: '대량 동시 발급 요청을 끝까지 처리했고 재고 정합성 검증도 통과했습니다.',
        detail: '동시 발급 후 최종 발급 건수와 잔여 재고가 초기 재고 수량과 일치했고, 서버 오류 없이 시나리오를 마쳤습니다.',
        action: '다음에는 ISSUE_BURST_STOCK 을 낮춰 oversubscription 상황에서도 같은 정합성이 유지되는지 확인해 보세요.',
      };
    case 'restaurant-issue-burst':
      return {
        summary: '맛집 쿠폰 동시 발급 요청을 끝까지 처리했고 재고 정합성 검증도 통과했습니다.',
        detail: '같은 restaurantId로 몰린 요청을 처리한 뒤 최종 발급 건수와 잔여 재고가 초기 수량과 일치했습니다.',
        action: '다음에는 ISSUE_BURST_STOCK 을 낮춰 oversubscription 상황에서도 같은 정합성이 유지되는지 확인해 보세요.',
      };
    case 'contention':
      return {
        summary: '동시 발급 요청을 큰 오류 없이 처리했습니다.',
        detail: '동시에 몰린 발급 요청이 시나리오 기준 안에서 처리됐고, 치명적인 실패 없이 종료됐습니다.',
        action: '경합이 더 심한 상황을 보고 싶다면 CONTENTION_VUS를 올려 다시 확인해 주세요.',
      };
    case 'issue-overload':
      return {
        summary: '쿠폰 발급 API 과부하 테스트를 끝까지 수행했습니다.',
        detail: '지속 부하 동안 오류율과 정상 응답 확인율이 기준 안에 들었고, 준비된 테스트 데이터 범위 안에서 정상 종료됐습니다.',
        action: '현재 수치를 기준선으로 남기고, 더 높은 사용자 수나 더 긴 시간으로 한 단계씩 올려 보세요.',
      };
    default:
      return {
        summary: '테스트를 끝까지 수행했습니다.',
        detail: '설정한 시나리오를 기준 안에서 정상 종료했습니다.',
        action: '이번 결과를 저장해 두고 다음 단계 부하 조건과 비교해 주세요.',
      };
  }
}

async function readSummary(summaryPath, startedAtMs) {
  try {
    const fileStat = await stat(summaryPath);
    if (fileStat.mtimeMs + 1000 < startedAtMs) {
      return null;
    }

    const contents = await readFile(summaryPath, 'utf8');
    return JSON.parse(contents);
  } catch (error) {
    return null;
  }
}

function buildScenarioExtraMetrics(scenario, summary) {
  const metricPrefix =
    scenario === 'issue-burst'
      ? 'issue_burst'
      : scenario === 'restaurant-issue-burst'
        ? 'restaurant_issue_burst'
        : null;

  if (metricPrefix == null) {
    return {
      textLines: [],
      blockFields: [],
    };
  }

  const metricOrZero = (key, nestedKey) => {
    const value = metricValue(summary, key, nestedKey);
    return value === 'n/a' ? 0 : value;
  };

  return {
    textLines: [
      `• *성공 발급 건수:* ${metricOrZero(`${metricPrefix}_success_count`, 'count')}`,
      `• *재고 부족 건수:* ${metricOrZero(`${metricPrefix}_out_of_stock_count`, 'count')}`,
      `• *전송 계층 오류 건수:* ${metricOrZero(`${metricPrefix}_transport_error_count`, 'count')}`,
      `• *예상 밖 응답 오류 건수:* ${metricOrZero(`${metricPrefix}_unexpected_client_error_count`, 'count')}`,
      `• *서버 오류 건수:* ${metricOrZero(`${metricPrefix}_server_error_count`, 'count')}`,
      `• *최종 발급 건수:* ${metricValue(summary, `${metricPrefix}_final_issued_count`)}`,
      `• *최종 잔여 재고:* ${metricValue(summary, `${metricPrefix}_final_remaining_quantity`)}`,
      `• *재고 정합성 검증:* ${formatRate(metricValue(summary, `${metricPrefix}_integrity_ok`, 'rate'))}`,
      `• *예상 결과 검증:* ${formatRate(metricValue(summary, `${metricPrefix}_expected_result_ok`, 'rate'))}`,
    ],
    blockFields: [
      {
        type: 'mrkdwn',
        text: `*성공 발급 건수*\n${metricOrZero(`${metricPrefix}_success_count`, 'count')}`,
      },
      {
        type: 'mrkdwn',
        text: `*재고 부족 건수*\n${metricOrZero(`${metricPrefix}_out_of_stock_count`, 'count')}`,
      },
      {
        type: 'mrkdwn',
        text: `*전송 계층 오류 건수*\n${metricOrZero(`${metricPrefix}_transport_error_count`, 'count')}`,
      },
      {
        type: 'mrkdwn',
        text: `*예상 밖 응답 오류 건수*\n${metricOrZero(`${metricPrefix}_unexpected_client_error_count`, 'count')}`,
      },
      {
        type: 'mrkdwn',
        text: `*서버 오류 건수*\n${metricOrZero(`${metricPrefix}_server_error_count`, 'count')}`,
      },
      {
        type: 'mrkdwn',
        text: `*최종 발급 건수*\n${metricValue(summary, `${metricPrefix}_final_issued_count`)}`,
      },
      {
        type: 'mrkdwn',
        text: `*최종 잔여 재고*\n${metricValue(summary, `${metricPrefix}_final_remaining_quantity`)}`,
      },
      {
        type: 'mrkdwn',
        text: `*재고 정합성 검증*\n${formatRate(metricValue(summary, `${metricPrefix}_integrity_ok`, 'rate'))}`,
      },
      {
        type: 'mrkdwn',
        text: `*예상 결과 검증*\n${formatRate(metricValue(summary, `${metricPrefix}_expected_result_ok`, 'rate'))}`,
      },
    ],
  };
}

function buildSlackMessage({
  profile,
  scenario,
  startedAt,
  finishedAt,
  summary,
  thresholdFailures,
  exitCode,
  failureReason,
  summaryPath,
  parsedEnv,
  envSource,
}) {
  const reportHeadline = '[부하테스트 내용 보고]';
  const status = exitCode === 0 && thresholdFailures.length === 0 ? '성공' : '실패';
  const scenarioLabel = scenarioLabels[scenario] || scenario;
  const loadSummary = buildLoadSummary(scenario, parsedEnv, envSource);
  const loadDescription = buildLoadDescription(scenario, parsedEnv, envSource);
  const thresholdSummary = thresholdFailures.map(formatThresholdFailure);
  const successExplanation =
    status === '성공'
      ? explainSuccess({
          scenario,
        })
      : null;
  const failureExplanation =
    status === '실패'
      ? explainFailure({
          failureReason,
          scenario,
          parsedEnv,
          envSource,
          thresholdFailures,
        })
      : null;
  const oneLineSummary =
    status === '성공' ? successExplanation.summary : failureExplanation.summary;
  const detailDescription =
    status === '성공' ? successExplanation.detail : failureExplanation.cause;
  const nextAction =
    status === '성공' ? successExplanation.action : failureExplanation.action;
  const p50 = formatDuration(metricValue(summary, 'http_req_duration', 'p(50)'));
  const p95 = formatDuration(metricValue(summary, 'http_req_duration', 'p(95)'));
  const p99 = formatDuration(metricValue(summary, 'http_req_duration', 'p(99)'));
  const errorRate = formatRate(metricValue(summary, 'http_req_failed', 'rate'));
  const checksRate = formatRate(metricValue(summary, 'checks', 'rate'));
  const scenarioExtraMetrics = buildScenarioExtraMetrics(scenario, summary);
  const thresholdText =
    thresholdSummary.length > 0
      ? `\n• *기준 미달 항목:* ${thresholdSummary.join(', ')}`
      : '';

  const text = [
    `[${profile} 환경 부하 테스트 내용]`,
    reportHeadline,
    `• *테스트 종류:* ${scenarioLabel}`,
    `• *결과:* ${status}`,
    `• *한 줄 요약:* ${oneLineSummary}`,
    `• *부하 조건:* ${loadSummary}`,
    `• *상세 설정:* ${loadDescription}`,
    `• *실행 시간:* ${startedAt.toISOString()} ~ ${finishedAt.toISOString()}`,
    `• *상세 설명:* ${detailDescription}`,
    `• *다음 조치:* ${nextAction}`,
    ...(thresholdSummary.length > 0 ? [`• *기준 미달 항목:* ${thresholdSummary.join(', ')}`] : []),
    `• *응답속도 중앙값(p50):* ${p50}`,
    `• *느린 요청 기준(p95):* ${p95}`,
    `• *매우 느린 요청 기준(p99):* ${p99}`,
    `• *오류율:* ${errorRate}`,
    `• *정상 응답 확인율:* ${checksRate}`,
    ...scenarioExtraMetrics.textLines,
    `• *결과 파일:* ${summaryPath}`,
  ].join('\n');

  const blocks = [
    {
      type: 'section',
      text: {
        type: 'mrkdwn',
        text: `*[${profile} 환경 부하 테스트 내용]*`,
      },
    },
    {
      type: 'section',
      text: {
        type: 'mrkdwn',
        text: `*${reportHeadline}*`,
      },
    },
    {
      type: 'divider',
    },
    {
      type: 'section',
      fields: [
        {
          type: 'mrkdwn',
          text: `*테스트 종류*\n${scenarioLabel}`,
        },
        {
          type: 'mrkdwn',
          text: `*결과*\n${status}`,
        },
        {
          type: 'mrkdwn',
          text: `*부하 조건*\n${loadSummary}`,
        },
        {
          type: 'mrkdwn',
          text: `*실행 시간*\n${startedAt.toISOString()} ~ ${finishedAt.toISOString()}`,
        },
      ],
    },
    {
      type: 'section',
      text: {
        type: 'mrkdwn',
        text: `*한 줄 요약*\n${oneLineSummary}`,
      },
    },
    {
      type: 'section',
      text: {
        type: 'mrkdwn',
        text:
          `*상세 설명*\n${detailDescription}\n\n` +
          `*다음 조치*\n${nextAction}` +
          thresholdText,
      },
    },
    {
      type: 'section',
      fields: [
        {
          type: 'mrkdwn',
          text: `*응답속도 중앙값(p50)*\n${p50}`,
        },
        {
          type: 'mrkdwn',
          text: `*느린 요청 기준(p95)*\n${p95}`,
        },
        {
          type: 'mrkdwn',
          text: `*매우 느린 요청 기준(p99)*\n${p99}`,
        },
        {
          type: 'mrkdwn',
          text: `*오류율*\n${errorRate}`,
        },
        {
          type: 'mrkdwn',
          text: `*정상 응답 확인율*\n${checksRate}`,
        },
      ],
    },
    ...(scenarioExtraMetrics.blockFields.length > 0
      ? [
          {
            type: 'section',
            fields: scenarioExtraMetrics.blockFields,
          },
        ]
      : []),
    {
      type: 'context',
      elements: [
        {
          type: 'mrkdwn',
          text: `*상세 설정:* \`${loadDescription}\``,
        },
        {
          type: 'mrkdwn',
          text: `*결과 파일:* \`${summaryPath}\``,
        },
      ],
    },
  ];

  return {
    status,
    text,
    blocks,
  };
}

function shouldNotify(notifyOn, status) {
  if (notifyOn === 'never') {
    return false;
  }

  if (notifyOn === 'always') {
    return true;
  }

  return status === '실패';
}

async function postSlackMessage(webhookUrl, message) {
  const target = new URL(webhookUrl);
  const payload = JSON.stringify({
    text: message.text,
    blocks: message.blocks,
  });
  const client = target.protocol === 'https:' ? https : http;

  await new Promise((resolve, reject) => {
    const request = client.request(
      target,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(payload),
        },
      },
      (response) => {
        const chunks = [];

        response.on('data', (chunk) => {
          chunks.push(chunk);
        });

        response.on('end', () => {
          const body = Buffer.concat(chunks).toString('utf8');

          if (response.statusCode && response.statusCode >= 200 && response.statusCode < 300) {
            resolve(body);
            return;
          }

          reject(
            new Error(
              `Slack webhook request failed: status=${response.statusCode} body=${body || '<empty>'}`,
            ),
          );
        });
      },
    );

    request.on('error', reject);
    request.write(payload);
    request.end();
  });
}

async function main() {
  const dotEnv = await loadDotEnv();
  const envSource = {
    ...dotEnv,
    ...process.env,
  };
  const parsed = parseArgs(process.argv.slice(2), envSource);
  const scenarioScript = resolveScenarioScript(parsed.scenario);
  const parsedEnv = parseK6EnvArgs(parsed.k6Args);
  const resultsDir = resolveResultsDir(parsedEnv, envSource);
  const summaryPath = path.resolve(resultsDir, `${parsed.scenario}-latest.json`);
  const startedAt = new Date();
  const startedAtMs = startedAt.getTime();
  const recentLines = [];
  const slowRequestSamples = [];
  const slowRequestSampleLimit = resolveSlowRequestSampleLimit(parsedEnv, envSource);

  await mkdir(resultsDir, { recursive: true });

  const child = spawn('k6', ['run', ...parsed.k6Args, scenarioScript], {
    cwd: process.cwd(),
    env: {
      ...envSource,
      SLOW_REQUEST_SAMPLE_STDOUT: '1',
    },
    stdio: ['inherit', 'pipe', 'pipe'],
  });

  const stdoutProcessor = createOutputProcessor(process.stdout.write.bind(process.stdout), recentLines, slowRequestSamples);
  const stderrProcessor = createOutputProcessor(process.stderr.write.bind(process.stderr), recentLines, slowRequestSamples);

  child.stdout.on('data', (data) => {
    stdoutProcessor.push(data);
  });

  child.stderr.on('data', (data) => {
    stderrProcessor.push(data);
  });

  const exitCode = await new Promise((resolve, reject) => {
    child.on('error', reject);
    child.on('close', (code) => resolve(code ?? 1));
  });

  const finishedAt = new Date();
  stdoutProcessor.flush();
  stderrProcessor.flush();
  const summary = await readSummary(summaryPath, startedAtMs);
  await writeSlowRequestSummary(
    resultsDir,
    parsed.scenario,
    finishedAt,
    slowRequestSampleLimit,
    slowRequestSamples,
  );
  const thresholdFailures = collectThresholdFailures(summary);
  const failureReason = extractFailureReason(recentLines);
  const slackMessage = buildSlackMessage({
    profile: parsed.profile,
    scenario: parsed.scenario,
    startedAt,
    finishedAt,
    summary,
    thresholdFailures,
    exitCode,
    failureReason,
    summaryPath: path.relative(process.cwd(), summaryPath),
    parsedEnv,
    envSource,
  });

  const generatedAt = finishedAt.toISOString().replace(/[:.]/g, '-');
  const slackPreviewLatestPath = path.resolve(resultsDir, `${parsed.scenario}-slack-latest.txt`);
  const slackPreviewTimestampPath = path.resolve(
    resultsDir,
    `${parsed.scenario}-slack-${generatedAt}.txt`,
  );

  await writeFile(slackPreviewLatestPath, slackMessage.text);
  await writeFile(slackPreviewTimestampPath, slackMessage.text);

  if (shouldNotify(parsed.notifyOn, slackMessage.status)) {
    if (parsed.webhookUrl) {
      await postSlackMessage(parsed.webhookUrl, slackMessage);
      console.log(`Slack notification sent: ${parsed.scenario} (${slackMessage.status})`);
    } else {
      console.warn(
        `Slack notification skipped because LOAD_TEST_SLACK_WEBHOOK is not set. Preview saved to ${path.relative(process.cwd(), slackPreviewLatestPath)}`,
      );
    }
  }

  process.exit(exitCode);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
