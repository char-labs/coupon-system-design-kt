import exec from 'k6/execution';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createVuEmail, signin, signupUser, waitForAdminSignin } from './lib/api.js';
import { buildCouponPayload, createCoupon, issueCoupon } from './lib/coupon.js';

export const options = {
  scenarios: {
    contention: {
      executor: 'per-vu-iterations',
      vus: config.contentionVus,
      iterations: 1,
      maxDuration: config.contentionMaxDuration,
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.05'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const coupon = createCoupon(
    adminToken.accessToken,
    buildCouponPayload({
      name: `k6-contention-${Date.now()}`,
      totalQuantity: Math.max(config.contentionVus + 10, 50),
    }),
  );

  return {
    couponId: coupon.id,
  };
}

export default function (data) {
  const email = createVuEmail('k6-contention');
  const name = `Contention User ${exec.vu.idInTest || 0}`;

  signupUser(email, config.testUserPassword, name);
  const userToken = signin(email, config.testUserPassword);
  issueCoupon(userToken.accessToken, data.couponId);
}

export function handleSummary(data) {
  return buildSummary('contention', data);
}
