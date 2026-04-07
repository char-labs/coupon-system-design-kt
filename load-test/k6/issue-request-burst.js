import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Gauge, Rate } from 'k6/metrics';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createSyntheticUserId, prepareSyntheticUsers, waitForAdminSignin } from './lib/api.js';
import {
  activateCoupon,
  buildCouponPayload,
  createCoupon,
  getCoupon,
  getCouponIssuePage,
  requestIssueCouponLoadTest,
  waitForCouponIssueRequestTerminalLoadTest,
} from './lib/coupon.js';

const issueRequestBurstAcceptedCount = new Counter('issue_request_burst_accepted_count');
const issueRequestBurstSucceededCount = new Counter('issue_request_burst_succeeded_count');
const issueRequestBurstOutOfStockCount = new Counter('issue_request_burst_out_of_stock_count');
const issueRequestBurstDeadCount = new Counter('issue_request_burst_dead_count');
const issueRequestBurstUnexpectedFailureCount = new Counter(
  'issue_request_burst_unexpected_failure_count',
);
const issueRequestBurstFinalIssuedCount = new Gauge('issue_request_burst_final_issued_count');
const issueRequestBurstFinalRemainingQuantity = new Gauge(
  'issue_request_burst_final_remaining_quantity',
);
const issueRequestBurstIntegrityOk = new Rate('issue_request_burst_integrity_ok');
const issueRequestBurstExpectedResultOk = new Rate('issue_request_burst_expected_result_ok');

export const options = {
  scenarios: {
    issueRequestBurst: {
      executor: 'per-vu-iterations',
      vus: config.issueRequestBurstVus,
      iterations: 1,
      maxDuration: config.issueRequestBurstMaxDuration,
    },
  },
  setupTimeout: config.issueRequestBurstSetupTimeout,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    issue_request_burst_dead_count: ['count==0'],
    issue_request_burst_unexpected_failure_count: ['count==0'],
    issue_request_burst_integrity_ok: ['rate==1'],
    issue_request_burst_expected_result_ok: ['rate==1'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  // Standard concurrency scenario: pre-create 1,000 synthetic users in setup,
  // then let all VUs hit the same coupon request API at the same time.
  prepareSyntheticUsers(config.issueRequestBurstVus);
  const runId = Date.now();
  const coupon = createCoupon(
    adminToken.accessToken,
    buildCouponPayload({
      name: `k6-issue-request-burst-${runId}`,
      totalQuantity: config.issueRequestBurstStock,
    }),
  );
  activateCoupon(adminToken.accessToken, coupon.id);

  return {
    couponId: coupon.id,
    totalQuantity: config.issueRequestBurstStock,
    expectedSuccessCount: Math.min(config.issueRequestBurstVus, config.issueRequestBurstStock),
    expectedRemainingQuantity: Math.max(
      config.issueRequestBurstStock - config.issueRequestBurstVus,
      0,
    ),
    allowOutOfStock: config.issueRequestBurstStock < config.issueRequestBurstVus,
  };
}

export default function (data) {
  const syntheticUserId = createSyntheticUserId(Math.max((exec.vu.idInTest || 1), 1));
  const request = requestIssueCouponLoadTest(data.couponId, syntheticUserId);
  issueRequestBurstAcceptedCount.add(1);

  const terminal = waitForCouponIssueRequestTerminalLoadTest(request.id);
  switch (terminal.status) {
    case 'SUCCEEDED':
      issueRequestBurstSucceededCount.add(1);
      check(terminal, {
        'issue_request_burst succeeded with couponIssueId': (result) =>
          Number(result.couponIssueId || 0) > 0,
      });
      return;
    case 'FAILED':
      if (terminal.resultCode === 'OUT_OF_STOCK' && data.allowOutOfStock) {
        issueRequestBurstOutOfStockCount.add(1);
        return;
      }

      issueRequestBurstUnexpectedFailureCount.add(1);
      check(terminal, {
        'issue_request_burst unexpected failed status': () => false,
      });
      return;
    case 'DEAD':
      issueRequestBurstDeadCount.add(1);
      check(terminal, {
        'issue_request_burst dead request': () => false,
      });
      return;
    default:
      issueRequestBurstUnexpectedFailureCount.add(1);
      check(terminal, {
        'issue_request_burst terminal status known': () => false,
      });
  }
}

export function teardown(data) {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const coupon = getCoupon(data.couponId, adminToken.accessToken);
  const couponIssuePage = getCouponIssuePage(adminToken.accessToken, data.couponId, 1);
  const issuedCount = Number(couponIssuePage.totalCount || 0);
  const remainingQuantity = Number(coupon.remainingQuantity || 0);
  const integrityOk = issuedCount + remainingQuantity === data.totalQuantity;
  const expectedResultOk =
    issuedCount === data.expectedSuccessCount &&
    remainingQuantity === data.expectedRemainingQuantity;

  issueRequestBurstFinalIssuedCount.add(issuedCount);
  issueRequestBurstFinalRemainingQuantity.add(remainingQuantity);
  issueRequestBurstIntegrityOk.add(integrityOk);
  issueRequestBurstExpectedResultOk.add(expectedResultOk);

  check(
    {
      integrityOk,
      expectedResultOk,
    },
    {
      'issue_request_burst stock invariant': (result) => result.integrityOk,
      'issue_request_burst expected issue result': (result) => result.expectedResultOk,
    },
  );
}

export function handleSummary(data) {
  return buildSummary('issue-request-burst', data);
}
