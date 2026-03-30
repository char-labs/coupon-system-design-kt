import exec from 'k6/execution';
import { sleep } from 'k6';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createVuEmail, signin, signupUser, waitForAdminSignin } from './lib/api.js';
import { buildCouponPayload, cancelCoupon, createCoupon, getMyCoupons, issueCoupon, useCoupon } from './lib/coupon.js';

let userSession;

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
  const coupon = createCoupon(
    adminToken.accessToken,
    buildCouponPayload({
      name: `k6-baseline-${Date.now()}`,
      totalQuantity: Math.max(config.baselineVus * 5000, 100000),
    }),
  );

  return {
    couponId: coupon.id,
  };
}

function ensureUserSession() {
  if (userSession) {
    return userSession;
  }

  const email = createVuEmail('k6-baseline');
  const name = `Baseline User ${exec.vu.idInTest || 0}`;

  signupUser(email, config.testUserPassword, name);
  userSession = signin(email, config.testUserPassword);
  return userSession;
}

export default function (data) {
  const token = ensureUserSession();
  const selector = exec.scenario.iterationInTest % 10;

  if (selector < 5) {
    const issue = issueCoupon(token.accessToken, data.couponId);
    useCoupon(token.accessToken, issue.id);
  } else if (selector < 8) {
    const issue = issueCoupon(token.accessToken, data.couponId);
    cancelCoupon(token.accessToken, issue.id);
  } else {
    getMyCoupons(token.accessToken);
  }

  sleep(1);
}

export function handleSummary(data) {
  return buildSummary('baseline', data);
}
