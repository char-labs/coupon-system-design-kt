import exec from 'k6/execution';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createVuEmail, signin, signupUser, waitForAdminSignin } from './lib/api.js';
import { buildCouponPayload, createCoupon, issueCoupon, useCoupon } from './lib/coupon.js';

export const options = {
  setupTimeout: '3m',
  scenarios: {
    smoke: {
      executor: 'per-vu-iterations',
      vus: config.smokeVus,
      iterations: 1,
      maxDuration: '1m',
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
      name: `k6-smoke-${Date.now()}`,
      totalQuantity: 10,
    }),
  );

  return {
    couponId: coupon.id,
  };
}

export default function (data) {
  const email = createVuEmail('k6-smoke');
  const name = `Smoke User ${exec.vu.idInTest || 0}`;

  signupUser(email, config.testUserPassword, name);
  const userToken = signin(email, config.testUserPassword);
  const issue = issueCoupon(userToken.accessToken, data.couponId);
  useCoupon(userToken.accessToken, issue.id);
}

export function handleSummary(data) {
  return buildSummary('smoke', data);
}
