import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { config } from './lib/config.js';
import { prepareUserSessions, waitForAdminSignin } from './lib/api.js';
import { acceptIssueCoupon, activateCoupon, buildCouponPayload, createCoupon } from './lib/coupon.js';
import { buildSummary } from './lib/summary.js';

const issueRealRampSuccessCount = new Counter('issue_real_ramp_success_count');

export const options = {
  scenarios: {
    warmupThenLoad: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: config.issueRampStage1Duration, target: config.issueRampStage1Target },
        { duration: config.issueRampStage2Duration, target: config.issueRampStage2Target },
        { duration: config.issueRampStage3Duration, target: config.issueRampStage3Target },
        { duration: config.issueRampStage4Duration, target: config.issueRampStage4Target },
        { duration: config.issueRampStage5Duration, target: config.issueRampStage5Target },
        { duration: config.issueRampStage6Duration, target: config.issueRampStage6Target },
        { duration: config.issueRampStage7Duration, target: config.issueRampStage7Target },
      ],
      gracefulStop: config.issueRampGracefulStop,
    },
  },
  setupTimeout: config.issueRealRampSetupTimeout,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_duration: ['p(95)<3000'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const couponIds = [];

  for (let index = 0; index < config.issueRealRampCouponPoolSize; index += 1) {
    const coupon = createCoupon(
      adminToken.accessToken,
      buildCouponPayload({
        name: `k6-issue-real-ramp-${runId}-${index}`,
        totalQuantity: Math.max(config.issueRealRampStockPerCoupon, 100),
      }),
    );
    activateCoupon(adminToken.accessToken, coupon.id);
    couponIds.push(coupon.id);
  }

  const preparedUsers = prepareUserSessions(
    config.issueRealRampUserPoolSize,
    {
      prefix: 'k6-issue-real-ramp-user',
      runId,
    },
  );

  return {
    couponIds,
    userTokens: preparedUsers.accessTokens,
  };
}

function selectIssueTarget(data) {
  const globalIteration = exec.scenario.iterationInTest || 0;
  const totalCapacity = data.userTokens.length * data.couponIds.length;

  if (globalIteration >= totalCapacity) {
    exec.test.abort(
      `issue_real_ramp capacity exhausted at iteration=${globalIteration + 1}. ` +
        `Increase ISSUE_REAL_RAMP_USER_POOL_SIZE or ISSUE_REAL_RAMP_COUPON_POOL_SIZE. ` +
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

  const request = acceptIssueCoupon(target.accessToken, target.couponId);
  if (request.result === 'SUCCESS') {
    issueRealRampSuccessCount.add(1);
  }

  check(request, {
    'issue_real_ramp immediate success': (result) => result.result === 'SUCCESS',
  });
}

export function handleSummary(data) {
  return buildSummary('issue-real-ramp', data);
}
