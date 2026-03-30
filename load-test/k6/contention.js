import exec from 'k6/execution';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { signin, signupUser, waitForAdminSignin } from './lib/api.js';
import { activateCoupon, buildCouponPayload, createCoupon, issueCoupon } from './lib/coupon.js';

export const options = {
  scenarios: {
    contention: {
      executor: 'per-vu-iterations',
      vus: config.contentionVus,
      iterations: 1,
      maxDuration: config.contentionMaxDuration,
    },
  },
  setupTimeout: '3m',
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
  activateCoupon(adminToken.accessToken, coupon.id);
  const userTokens = [];
  const runId = Date.now();

  for (let index = 0; index < config.contentionVus; index += 1) {
    const email = `k6-contention-setup+${runId}-${index}@coupon.local`;
    const name = `Contention User ${index + 1}`;

    signupUser(email, config.testUserPassword, name);
    const userToken = signin(email, config.testUserPassword);
    userTokens.push(userToken.accessToken);
  }

  return {
    couponId: coupon.id,
    userTokens,
  };
}

export default function (data) {
  const tokenIndex = Math.max((exec.vu.idInTest || 1) - 1, 0);
  issueCoupon(data.userTokens[tokenIndex], data.couponId);
}

export function handleSummary(data) {
  return buildSummary('contention', data);
}
