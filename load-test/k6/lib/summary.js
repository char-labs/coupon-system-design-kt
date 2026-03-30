import { config } from './config.js';

function metricValue(data, key, nestedKey) {
  const metric = data.metrics[key];
  if (!metric) {
    return 'n/a';
  }

  if (nestedKey) {
    return metric.values?.[nestedKey] ?? 'n/a';
  }

  return metric.values?.value ?? 'n/a';
}

export function buildSummary(name, data) {
  const generatedAt = new Date().toISOString();
  const timestamp = generatedAt.replace(/[:.]/g, '-');
  const summary = {
    scenario: name,
    generatedAt,
    state: data.state,
    metrics: data.metrics,
  };

  const stdout = [
    `scenario: ${name}`,
    `http_req_duration p50: ${metricValue(data, 'http_req_duration', 'p(50)')}`,
    `http_req_duration p95: ${metricValue(data, 'http_req_duration', 'p(95)')}`,
    `http_req_duration p99: ${metricValue(data, 'http_req_duration', 'p(99)')}`,
    `http_req_failed rate: ${metricValue(data, 'http_req_failed', 'rate')}`,
    `checks rate: ${metricValue(data, 'checks', 'rate')}`,
  ].join('\n');

  return {
    stdout,
    [`${config.resultsDir}/${name}-latest.json`]: JSON.stringify(summary, null, 2),
    [`${config.resultsDir}/${name}-${timestamp}.json`]: JSON.stringify(summary, null, 2),
  };
}
