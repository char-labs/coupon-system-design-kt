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

function buildExtraLines(name, data) {
  if (name !== 'issue-burst') {
    return [];
  }

  const metricOrZero = (key, nestedKey) => {
    const value = metricValue(data, key, nestedKey);
    return value === 'n/a' ? 0 : value;
  };

  return [
    `issue_burst success count: ${metricOrZero('issue_burst_success_count', 'count')}`,
    `issue_burst out_of_stock count: ${metricOrZero('issue_burst_out_of_stock_count', 'count')}`,
    `issue_burst unexpected client error count: ${metricOrZero('issue_burst_unexpected_client_error_count', 'count')}`,
    `issue_burst server error count: ${metricOrZero('issue_burst_server_error_count', 'count')}`,
    `issue_burst final issued count: ${metricValue(data, 'issue_burst_final_issued_count')}`,
    `issue_burst final remaining quantity: ${metricValue(data, 'issue_burst_final_remaining_quantity')}`,
    `issue_burst integrity ok rate: ${metricValue(data, 'issue_burst_integrity_ok', 'rate')}`,
    `issue_burst expected result ok rate: ${metricValue(data, 'issue_burst_expected_result_ok', 'rate')}`,
  ];
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
    ...buildExtraLines(name, data),
  ].join('\n');

  return {
    stdout,
    [`${config.resultsDir}/${name}-latest.json`]: JSON.stringify(summary, null, 2),
    [`${config.resultsDir}/${name}-${timestamp}.json`]: JSON.stringify(summary, null, 2),
  };
}
