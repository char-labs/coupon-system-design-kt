import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createSyntheticUserId, prepareSyntheticUsers, waitForAdminSignin } from './lib/api.js';
import {
  activateCoupon,
  buildCouponPayload,
  createCoupon,
  requestIssueCouponLoadTest,
} from './lib/coupon.js';

const issueRequestRampAcceptedCount = new Counter('issue_request_ramp_accepted_count');

export const options = {
  scenarios: {
    warmupThenLoad: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: config.issueRequestRampStage1Duration, target: config.issueRequestRampStage1Target },
        { duration: config.issueRequestRampStage2Duration, target: config.issueRequestRampStage2Target },
        { duration: config.issueRequestRampStage3Duration, target: config.issueRequestRampStage3Target },
        { duration: config.issueRequestRampStage4Duration, target: config.issueRequestRampStage4Target },
        { duration: config.issueRequestRampStage5Duration, target: config.issueRequestRampStage5Target },
        { duration: config.issueRequestRampStage6Duration, target: config.issueRequestRampStage6Target },
        { duration: config.issueRequestRampStage7Duration, target: config.issueRequestRampStage7Target },
      ],
      gracefulStop: config.issueRequestRampGracefulStop,
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_duration: ['p(95)<3000'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const couponIds = [];

  prepareSyntheticUsers(config.issueRequestRampUserPoolSize);

  for (let index = 0; index < config.issueRequestRampCouponPoolSize; index += 1) {
    const coupon = createCoupon(
      adminToken.accessToken,
      buildCouponPayload({
        name: `k6-issue-request-ramp-${runId}-${index}`,
        totalQuantity: Math.max(config.issueRequestRampStockPerCoupon, 100),
      }),
    );
    activateCoupon(adminToken.accessToken, coupon.id);
    couponIds.push(coupon.id);
  }

  return {
    couponIds,
    userPoolSize: config.issueRequestRampUserPoolSize,
  };
}

function selectIssueTarget(data) {
  const globalIteration = exec.scenario.iterationInTest || 0;
  const totalCapacity = data.userPoolSize * data.couponIds.length;

  if (globalIteration >= totalCapacity) {
    exec.test.abort(
      `issue_request_ramp capacity exhausted at iteration=${globalIteration + 1}. ` +
        `Increase ISSUE_REQUEST_RAMP_USER_POOL_SIZE or ISSUE_REQUEST_RAMP_COUPON_POOL_SIZE. ` +
        `Current capacity=${totalCapacity} (${data.userPoolSize} users x ${data.couponIds.length} coupons)`,
    );
    return null;
  }

  const userIndex = globalIteration % data.userPoolSize;
  const couponIndex = Math.floor(globalIteration / data.userPoolSize);

  return {
    syntheticUserId: createSyntheticUserId(userIndex + 1),
    couponId: data.couponIds[couponIndex],
  };
}

export default function (data) {
  sleep(1);

  const target = selectIssueTarget(data);
  if (!target) {
    return;
  }

  const response = requestIssueCouponLoadTest(target.couponId, target.syntheticUserId);
  issueRequestRampAcceptedCount.add(1);

  check(response, {
    'issue_request_ramp accepted': (request) => request.status === 'PENDING',
  });
}

export function handleSummary(data) {
  return buildSummary('issue-request-ramp', data);
}
