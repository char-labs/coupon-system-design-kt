import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createSyntheticUserId, prepareSyntheticUsers, waitForAdminSignin } from './lib/api.js';
import {
  activateCoupon,
  buildCouponPayload,
  createCoupon,
  requestIssueCouponLoadTest,
  waitForCouponIssueRequestTerminalLoadTest,
} from './lib/coupon.js';

const issueRequestOverloadAcceptedCount = new Counter('issue_request_overload_accepted_count');
const issueRequestOverloadSucceededCount = new Counter('issue_request_overload_succeeded_count');
const issueRequestOverloadDeadCount = new Counter('issue_request_overload_dead_count');
const issueRequestOverloadUnexpectedFailureCount = new Counter(
  'issue_request_overload_unexpected_failure_count',
);

export const options = {
  scenarios: {
    issueRequestOverload: {
      executor: 'constant-vus',
      vus: config.issueRequestOverloadVus,
      duration: config.issueRequestOverloadDuration,
      gracefulStop: '30s',
    },
  },
  setupTimeout: config.issueRequestOverloadSetupTimeout,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.05'],
    issue_request_overload_dead_count: ['count==0'],
    issue_request_overload_unexpected_failure_count: ['count==0'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const couponIds = [];

  prepareSyntheticUsers(config.issueRequestOverloadUserPoolSize);

  for (let index = 0; index < config.issueRequestOverloadCouponPoolSize; index += 1) {
    const coupon = createCoupon(
      adminToken.accessToken,
      buildCouponPayload({
        name: `k6-issue-request-overload-${runId}-${index}`,
        totalQuantity: Math.max(config.issueRequestOverloadStockPerCoupon, 100),
      }),
    );
    activateCoupon(adminToken.accessToken, coupon.id);
    couponIds.push(coupon.id);
  }

  return {
      couponIds,
      userPoolSize: config.issueRequestOverloadUserPoolSize,
  };
}

function selectIssueTarget(data) {
  const globalIteration = exec.scenario.iterationInTest || 0;
  const totalCapacity = data.userPoolSize * data.couponIds.length;

  if (globalIteration >= totalCapacity) {
    exec.test.abort(
      `issue_request_overload capacity exhausted at iteration=${globalIteration + 1}. ` +
        `Increase ISSUE_REQUEST_OVERLOAD_USER_POOL_SIZE or ISSUE_REQUEST_OVERLOAD_COUPON_POOL_SIZE. ` +
        `Current capacity=${totalCapacity} (${data.userPoolSize} users x ${data.couponIds.length} coupons)`,
    );
    return null;
  }

  const userIndex = globalIteration % data.userPoolSize;
  const couponIndex = Math.floor(globalIteration / data.userPoolSize) % data.couponIds.length;

  return {
    userId: createSyntheticUserId(userIndex + 1),
    couponId: data.couponIds[couponIndex],
  };
}

export default function (data) {
  const target = selectIssueTarget(data);
  if (!target) {
    return;
  }

  const request = requestIssueCouponLoadTest(target.couponId, target.userId);
  issueRequestOverloadAcceptedCount.add(1);

  const terminal = waitForCouponIssueRequestTerminalLoadTest(request.id);
  switch (terminal.status) {
    case 'SUCCEEDED':
      issueRequestOverloadSucceededCount.add(1);
      check(terminal, {
        'issue_request_overload succeeded with couponIssueId': (result) =>
          Number(result.couponIssueId || 0) > 0,
      });
      return;
    case 'DEAD':
      issueRequestOverloadDeadCount.add(1);
      check(terminal, {
        'issue_request_overload dead request': () => false,
      });
      return;
    default:
      issueRequestOverloadUnexpectedFailureCount.add(1);
      check(terminal, {
        'issue_request_overload unexpected terminal status': () => false,
      });
  }
}

export function handleSummary(data) {
  return buildSummary('issue-request-overload', data);
}
