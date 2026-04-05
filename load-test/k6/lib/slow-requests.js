import exec from 'k6/execution';
import { config } from './config.js';

export const SLOW_REQUEST_SAMPLE_MARKER = '__K6_SLOW_REQUEST_SAMPLE__';

const vuSlowRequestCandidates = [];

function trimPreview(body) {
  if (!body) {
    return '<empty>';
  }

  const normalized = String(body).replace(/\s+/g, ' ').trim();
  if (!normalized) {
    return '<empty>';
  }

  if (normalized.length <= config.slowRequestPreviewMaxLength) {
    return normalized;
  }

  return `${normalized.slice(0, config.slowRequestPreviewMaxLength)}...`;
}

function durationMsOf(response) {
  const duration = response?.timings?.duration;
  if (typeof duration !== 'number' || !Number.isFinite(duration)) {
    return 0;
  }

  return Number(duration.toFixed(3));
}

function sortSlowFirst(left, right) {
  return right.durationMs - left.durationMs;
}

function scenarioName() {
  try {
    return exec.scenario.name || 'unknown';
  } catch (error) {
    return 'teardown';
  }
}

export function maybeRecordSlowRequestSample(response, options = {}) {
  const {
    requestName,
    requestGroup = 'business',
  } = options || {};

  if (!config.slowRequestSampleStdout || !requestName || requestGroup !== 'business') {
    return;
  }

  const sample = {
    requestName,
    requestGroup,
    scenario: scenarioName(),
    status: response?.status ?? 0,
    durationMs: durationMsOf(response),
    timestamp: new Date().toISOString(),
    responsePreview: trimPreview(response?.body),
  };

  const limit = Math.max(config.slowRequestSampleLimit, 1);
  const minTrackedDuration =
    vuSlowRequestCandidates.length < limit
      ? -1
      : vuSlowRequestCandidates[vuSlowRequestCandidates.length - 1].durationMs;

  if (sample.durationMs <= minTrackedDuration) {
    return;
  }

  vuSlowRequestCandidates.push(sample);
  vuSlowRequestCandidates.sort(sortSlowFirst);
  vuSlowRequestCandidates.splice(limit);

  console.log(`${SLOW_REQUEST_SAMPLE_MARKER}${encodeURIComponent(JSON.stringify(sample))}`);
}
