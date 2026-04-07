import exec from 'k6/execution';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { prepareUserSessions, waitForAdminSignin } from './lib/api.js';
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
  const preparedUsers = prepareUserSessions(
    config.contentionVus,
    { prefix: 'k6-contention-user' },
  );

  return {
    couponId: coupon.id,
    userTokens: preparedUsers.accessTokens,
  };
}

export default function (data) {
  const accessToken = data.userTokens[Math.max((exec.vu.idInTest || 1) - 1, 0)];
  issueCoupon(accessToken, data.couponId);
}

export function handleSummary(data) {
  return buildSummary('contention', data);
}
