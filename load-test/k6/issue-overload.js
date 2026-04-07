import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { prepareUserSessions, waitForAdminSignin } from './lib/api.js';
import {
  acceptIssueCoupon,
  activateCoupon,
  buildCouponPayload,
  createCoupon,
} from './lib/coupon.js';

const issueOverloadSuccessCount = new Counter('issue_overload_success_count');
const issueOverloadSoldOutCount = new Counter('issue_overload_sold_out_count');
const issueOverloadUnexpectedFailureCount = new Counter('issue_overload_unexpected_failure_count');

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
    issue_overload_unexpected_failure_count: ['count==0'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const couponIds = [];
  const preparedUsers = prepareUserSessions(
    config.issueOverloadUserPoolSize,
    { prefix: 'k6-issue-overload-user' },
  );

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

  const issueResult = acceptIssueCoupon(target.accessToken, target.couponId);
  if (issueResult.result !== 'SUCCESS') {
    if (issueResult.result === 'SOLD_OUT') {
      issueOverloadSoldOutCount.add(1);
      check(issueResult, {
        'issue_overload should not be sold out': () => false,
      });
      return;
    }

    if (issueResult.result === 'DUPLICATE') {
      issueOverloadUnexpectedFailureCount.add(1);
      check(issueResult, {
        'issue_overload duplicate should not happen': () => false,
      });
      return;
    }

    issueOverloadUnexpectedFailureCount.add(1);
    check(issueResult, {
      'issue_overload unexpected issue result': () => false,
    });
    return;
  }

  issueOverloadSuccessCount.add(1);
  check(issueResult, {
    'issue_overload success should be accepted': (result) =>
      result.result === 'SUCCESS' && result.status === 202,
  });
}

export function handleSummary(data) {
  return buildSummary('issue-overload', data);
}
