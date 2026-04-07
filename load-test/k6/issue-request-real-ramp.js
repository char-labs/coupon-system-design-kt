import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { config } from './lib/config.js';
import { createUserSession, waitForAdminSignin } from './lib/api.js';
import { activateCoupon, buildCouponPayload, createCoupon, requestIssueCoupon } from './lib/coupon.js';
import { buildSummary } from './lib/summary.js';

const issueRequestRealRampAcceptedCount = new Counter('issue_request_real_ramp_accepted_count');

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
  setupTimeout: config.issueRequestRealRampSetupTimeout,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_duration: ['p(95)<3000'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const couponIds = [];
  const userTokens = [];

  for (let index = 0; index < config.issueRequestRealRampCouponPoolSize; index += 1) {
    const coupon = createCoupon(
      adminToken.accessToken,
      buildCouponPayload({
        name: `k6-issue-request-real-ramp-${runId}-${index}`,
        totalQuantity: Math.max(config.issueRequestRealRampStockPerCoupon, 100),
      }),
    );
    activateCoupon(adminToken.accessToken, coupon.id);
    couponIds.push(coupon.id);
  }

  for (let index = 0; index < config.issueRequestRealRampUserPoolSize; index += 1) {
    const email = `k6-issue-request-real-ramp+${runId}-${index}@coupon.local`;
    const name = `Issue Request Real Ramp User ${index + 1}`;
    userTokens.push(createUserSession(email, name));
  }

  return {
    couponIds,
    userTokens,
  };
}

function selectIssueTarget(data) {
  const globalIteration = exec.scenario.iterationInTest || 0;
  const totalCapacity = data.userTokens.length * data.couponIds.length;

  if (globalIteration >= totalCapacity) {
    exec.test.abort(
      `issue_request_real_ramp capacity exhausted at iteration=${globalIteration + 1}. ` +
        `Increase ISSUE_REQUEST_REAL_RAMP_USER_POOL_SIZE or ISSUE_REQUEST_REAL_RAMP_COUPON_POOL_SIZE. ` +
        `Current capacity=${totalCapacity} (` +
        `${data.userTokens.length} users x ${data.couponIds.length} coupons)`,
    );
    return null;
  }

  const userIndex = globalIteration % data.userTokens.length;
  const couponIndex = Math.floor(globalIteration / data.userTokens.length);

  return {
    accessToken: data.userTokens[userIndex],
    couponId: data.couponIds[couponIndex],
  };
}

export default function (data) {
  sleep(1);

  const target = selectIssueTarget(data);
  if (!target) {
    return;
  }

  const request = requestIssueCoupon(target.accessToken, target.couponId);
  issueRequestRealRampAcceptedCount.add(1);

  check(request, {
    'issue_request_real_ramp accepted': (result) => result.status === 'PENDING',
  });
}

export function handleSummary(data) {
  return buildSummary('issue-request-real-ramp', data);
}
