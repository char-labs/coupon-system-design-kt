import exec from 'k6/execution';
import { sleep } from 'k6';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createVuEmail, signin, signupUser, waitForAdminSignin } from './lib/api.js';
import { activateCoupon, buildCouponPayload, cancelCoupon, createCoupon, getMyCoupons, issueCoupon, useCoupon } from './lib/coupon.js';

const userSessions = [];

export const options = {
  scenarios: {
    baseline: {
      executor: 'constant-vus',
      vus: config.baselineVus,
      duration: config.baselineDuration,
      gracefulStop: '30s',
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const couponIds = [];

  for (let index = 0; index < config.baselineCouponPoolSize; index += 1) {
    const coupon = createCoupon(
      adminToken.accessToken,
      buildCouponPayload({
        name: `k6-baseline-${Date.now()}-${index}`,
        totalQuantity: Math.max(config.baselineVus * config.baselineSessionPoolSize, 10000),
      }),
    );
    activateCoupon(adminToken.accessToken, coupon.id);
    couponIds.push(coupon.id);
  }

  return {
    couponIds,
  };
}

function ensureUserSession(sessionIndex) {
  if (userSessions[sessionIndex]) {
    return userSessions[sessionIndex];
  }

  const email = createVuEmail(`k6-baseline-${sessionIndex}`);
  const name = `Baseline User ${exec.vu.idInTest || 0}-${sessionIndex}`;

  signupUser(email, config.testUserPassword, name);
  userSessions[sessionIndex] = signin(email, config.testUserPassword);
  return userSessions[sessionIndex];
}

function selectIssueTarget(data) {
  const localIteration = exec.vu.iterationInScenario || 0;
  const couponIndex = localIteration % data.couponIds.length;
  const sessionIndex =
    Math.floor(localIteration / data.couponIds.length) % config.baselineSessionPoolSize;

  return {
    couponId: data.couponIds[couponIndex],
    token: ensureUserSession(sessionIndex),
  };
}

export default function (data) {
  const selector = exec.scenario.iterationInTest % 10;

  if (selector < 5) {
    const target = selectIssueTarget(data);
    const issue = issueCoupon(target.token.accessToken, target.couponId);
    useCoupon(target.token.accessToken, issue.id);
  } else if (selector < 8) {
    const target = selectIssueTarget(data);
    const issue = issueCoupon(target.token.accessToken, target.couponId);
    cancelCoupon(target.token.accessToken, issue.id);
  } else {
    getMyCoupons(ensureUserSession(0).accessToken);
  }

  sleep(1);
}

export function handleSummary(data) {
  return buildSummary('baseline', data);
}
