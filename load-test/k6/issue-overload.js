import exec from 'k6/execution';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { signin, signupUser, waitForAdminSignin } from './lib/api.js';
import { activateCoupon, buildCouponPayload, createCoupon, issueCoupon } from './lib/coupon.js';

export const options = {
  scenarios: {
    issueOverload: {
      executor: 'constant-vus',
      vus: config.issueOverloadVus,
      duration: config.issueOverloadDuration,
      gracefulStop: '30s',
    },
  },
  setupTimeout: config.issueOverloadSetupTimeout,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.05'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const couponIds = [];
  const userTokens = [];

  for (let index = 0; index < config.issueOverloadCouponPoolSize; index += 1) {
    const coupon = createCoupon(
      adminToken.accessToken,
      buildCouponPayload({
        name: `k6-issue-overload-${runId}-${index}`,
        totalQuantity: Math.max(config.issueOverloadUserPoolSize + 10, 100),
      }),
    );
    activateCoupon(adminToken.accessToken, coupon.id);
    couponIds.push(coupon.id);
  }

  for (let index = 0; index < config.issueOverloadUserPoolSize; index += 1) {
    const email = `k6-issue-overload+${runId}-${index}@coupon.local`;
    const name = `Issue Overload User ${index + 1}`;

    signupUser(email, config.testUserPassword, name);
    const userToken = signin(email, config.testUserPassword);
    userTokens.push(userToken.accessToken);
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
      `issue_overload capacity exhausted at iteration=${globalIteration + 1}. ` +
        `Increase ISSUE_OVERLOAD_USER_POOL_SIZE or ISSUE_OVERLOAD_COUPON_POOL_SIZE. ` +
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
  const target = selectIssueTarget(data);
  if (!target) {
    return;
  }
  issueCoupon(target.accessToken, target.couponId);
}

export function handleSummary(data) {
  return buildSummary('issue-overload', data);
}
