import exec from 'k6/execution';
import { check } from 'k6';
import { config } from './lib/config.js';
import { buildSummary } from './lib/summary.js';
import { createSyntheticUserId, prepareSyntheticUsers, waitForAdminSignin } from './lib/api.js';
import {
  activateCoupon,
  buildCouponPayload,
  createCoupon,
  requestIssueCouponLoadTest,
  waitForCouponIssueRequestTerminalLoadTest,
} from './lib/coupon.js';

export const options = {
  setupTimeout: '3m',
  scenarios: {
    issueRequestSmoke: {
      executor: 'per-vu-iterations',
      vus: config.issueRequestSmokeVus,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  prepareSyntheticUsers(config.issueRequestSmokeVus);
  const coupon = createCoupon(
    adminToken.accessToken,
    buildCouponPayload({
      name: `k6-issue-request-smoke-${Date.now()}`,
      totalQuantity: 10,
    }),
  );
  activateCoupon(adminToken.accessToken, coupon.id);

  return {
    couponId: coupon.id,
  };
}

export default function (data) {
  const syntheticUserId = createSyntheticUserId(exec.vu.idInTest || 1);
  const request = requestIssueCouponLoadTest(data.couponId, syntheticUserId);
  const terminal = waitForCouponIssueRequestTerminalLoadTest(request.id);

  check(terminal, {
    'issue_request_smoke succeeded': (result) =>
      result.status === 'SUCCEEDED' && Number(result.couponIssueId || 0) > 0,
  });
}

export function handleSummary(data) {
  return buildSummary('issue-request-smoke', data);
}
