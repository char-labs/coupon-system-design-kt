import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Gauge, Rate } from 'k6/metrics';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { prepareUserSessions, waitForAdminSignin } from './lib/api.js';
import {
  activateCoupon,
  buildCouponPayload,
  createCoupon,
  tryIssueCoupon,
  waitForCouponIssueSettlement,
} from './lib/coupon.js';

const issueBurstSuccessCount = new Counter('issue_burst_success_count');
const issueBurstOutOfStockCount = new Counter('issue_burst_out_of_stock_count');
const issueBurstUnexpectedClientErrorCount = new Counter(
  'issue_burst_unexpected_client_error_count',
);
const issueBurstServerErrorCount = new Counter('issue_burst_server_error_count');
const issueBurstFinalIssuedCount = new Gauge('issue_burst_final_issued_count');
const issueBurstFinalRemainingQuantity = new Gauge(
  'issue_burst_final_remaining_quantity',
);
const issueBurstIntegrityOk = new Rate('issue_burst_integrity_ok');
const issueBurstExpectedResultOk = new Rate('issue_burst_expected_result_ok');

export const options = {
  scenarios: {
    issueBurst: {
      executor: 'per-vu-iterations',
      vus: config.issueBurstVus,
      iterations: 1,
      maxDuration: config.issueBurstMaxDuration,
    },
  },
  setupTimeout: config.issueBurstSetupTimeout,
  teardownTimeout: `${config.issueSettlementTimeoutSeconds + 30}s`,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    issue_burst_unexpected_client_error_count: ['count==0'],
    issue_burst_server_error_count: ['count==0'],
    issue_burst_integrity_ok: ['rate==1'],
    issue_burst_expected_result_ok: ['rate==1'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const coupon = createCoupon(
    adminToken.accessToken,
    buildCouponPayload({
      name: `k6-issue-burst-${runId}`,
      totalQuantity: config.issueBurstStock,
    }),
  );
  activateCoupon(adminToken.accessToken, coupon.id);
  const preparedUsers = prepareUserSessions(
    config.issueBurstVus,
    { prefix: 'k6-issue-burst-user' },
  );

  return {
    adminAccessToken: adminToken.accessToken,
    couponId: coupon.id,
    userTokens: preparedUsers.accessTokens,
    totalQuantity: config.issueBurstStock,
    expectedSuccessCount: Math.min(config.issueBurstVus, config.issueBurstStock),
    expectedRemainingQuantity: Math.max(config.issueBurstStock - config.issueBurstVus, 0),
    allowOutOfStock: config.issueBurstStock < config.issueBurstVus,
  };
}

export default function (data) {
  const accessToken = data.userTokens[Math.max((exec.vu.idInTest || 1) - 1, 0)];
  const outcome = tryIssueCoupon(
    accessToken,
    data.couponId,
    {
      allowOutOfStock: data.allowOutOfStock,
      label: 'issue_burst_issue_coupon',
    },
  );

  switch (outcome.outcome) {
    case 'SUCCESS':
      issueBurstSuccessCount.add(1);
      return;
    case 'OUT_OF_STOCK':
      issueBurstOutOfStockCount.add(1);
      if (!data.allowOutOfStock) {
        issueBurstUnexpectedClientErrorCount.add(1);
        check(outcome, {
          'issue_burst unexpected out_of_stock': () => false,
        });
      }
      return;
    case 'UNEXPECTED_ERROR':
    case 'UNEXPECTED_RESPONSE':
    default:
      if (outcome.status >= 500) {
        issueBurstServerErrorCount.add(1);
      } else {
        issueBurstUnexpectedClientErrorCount.add(1);
      }
      check(outcome, {
        'issue_burst unexpected response': () => false,
      });
      return;
  }
}

export function teardown(data) {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const snapshot = waitForCouponIssueSettlement(
    adminToken.accessToken,
    data.couponId,
    {
      totalQuantity: data.totalQuantity,
      expectedIssuedCount: data.expectedSuccessCount,
      expectedRemainingQuantity: data.expectedRemainingQuantity,
    },
  );
  const issuedCount = snapshot.issuedCount;
  const remainingQuantity = snapshot.remainingQuantity;
  const integrityOk = snapshot.integrityOk;
  const expectedResultOk = snapshot.expectedResultOk;

  issueBurstFinalIssuedCount.add(issuedCount);
  issueBurstFinalRemainingQuantity.add(remainingQuantity);
  issueBurstIntegrityOk.add(integrityOk);
  issueBurstExpectedResultOk.add(expectedResultOk);

  check(
    {
      integrityOk,
      expectedResultOk,
    },
    {
      'issue_burst stock invariant': (result) => result.integrityOk,
      'issue_burst expected issue result': (result) => result.expectedResultOk,
    },
  );
}

export function handleSummary(data) {
  return buildSummary('issue-burst', data);
}
