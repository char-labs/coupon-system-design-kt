import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Gauge, Rate } from 'k6/metrics';
import { config } from './lib/config.js';
import { prepareUserSessions, waitForAdminSignin } from './lib/api.js';
import { buildSummary } from './lib/summary.js';
import {
  activateCoupon,
  buildCouponPayload,
  buildRestaurantCouponPayload,
  createCoupon,
  createRestaurantCoupons,
  tryIssueRestaurantCoupon,
  waitForCouponIssueSettlement,
} from './lib/coupon.js';

const restaurantIssueBurstSuccessCount = new Counter('restaurant_issue_burst_success_count');
const restaurantIssueBurstOutOfStockCount = new Counter('restaurant_issue_burst_out_of_stock_count');
const restaurantIssueBurstUnexpectedClientErrorCount = new Counter(
  'restaurant_issue_burst_unexpected_client_error_count',
);
const restaurantIssueBurstServerErrorCount = new Counter(
  'restaurant_issue_burst_server_error_count',
);
const restaurantIssueBurstFinalIssuedCount = new Gauge('restaurant_issue_burst_final_issued_count');
const restaurantIssueBurstFinalRemainingQuantity = new Gauge(
  'restaurant_issue_burst_final_remaining_quantity',
);
const restaurantIssueBurstIntegrityOk = new Rate('restaurant_issue_burst_integrity_ok');
const restaurantIssueBurstExpectedResultOk = new Rate(
  'restaurant_issue_burst_expected_result_ok',
);

export const options = {
  scenarios: {
    restaurantIssueBurst: {
      executor: 'per-vu-iterations',
      vus: config.issueBurstVus,
      iterations: 1,
      maxDuration: config.issueBurstMaxDuration,
    },
  },
  setupTimeout: config.issueBurstSetupTimeout,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    restaurant_issue_burst_unexpected_client_error_count: ['count==0'],
    restaurant_issue_burst_server_error_count: ['count==0'],
    restaurant_issue_burst_integrity_ok: ['rate==1'],
    restaurant_issue_burst_expected_result_ok: ['rate==1'],
  },
};

export function setup() {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const runId = Date.now();
  const restaurantId = 101;
  const coupon = createCoupon(
    adminToken.accessToken,
    buildCouponPayload({
      name: `k6-restaurant-issue-burst-${runId}`,
      totalQuantity: config.issueBurstStock,
    }),
  );
  activateCoupon(adminToken.accessToken, coupon.id);
  createRestaurantCoupons(adminToken.accessToken, [
    buildRestaurantCouponPayload({
      restaurantId,
      couponId: coupon.id,
    }),
  ]);
  const preparedUsers = prepareUserSessions(
    config.issueBurstVus,
    { prefix: 'k6-restaurant-issue-burst-user' },
  );

  return {
    couponId: coupon.id,
    restaurantId,
    userTokens: preparedUsers.accessTokens,
    totalQuantity: config.issueBurstStock,
    expectedSuccessCount: Math.min(config.issueBurstVus, config.issueBurstStock),
    expectedRemainingQuantity: Math.max(config.issueBurstStock - config.issueBurstVus, 0),
    allowOutOfStock: config.issueBurstStock < config.issueBurstVus,
  };
}

export default function (data) {
  const accessToken = data.userTokens[Math.max((exec.vu.idInTest || 1) - 1, 0)];
  const outcome = tryIssueRestaurantCoupon(accessToken, data.restaurantId, {
    allowOutOfStock: data.allowOutOfStock,
  });

  switch (outcome.outcome) {
    case 'SUCCESS':
      restaurantIssueBurstSuccessCount.add(1);
      return;
    case 'OUT_OF_STOCK':
      restaurantIssueBurstOutOfStockCount.add(1);
      if (!data.allowOutOfStock) {
        restaurantIssueBurstUnexpectedClientErrorCount.add(1);
        check(outcome, {
          'restaurant_issue_burst unexpected out_of_stock': () => false,
        });
      }
      return;
    case 'UNEXPECTED_ERROR':
    case 'UNEXPECTED_RESPONSE':
    default:
      if (outcome.status >= 500) {
        restaurantIssueBurstServerErrorCount.add(1);
      } else {
        restaurantIssueBurstUnexpectedClientErrorCount.add(1);
      }
      check(outcome, {
        'restaurant_issue_burst unexpected response': () => false,
      });
      return;
  }
}

export function teardown(data) {
  const adminToken = waitForAdminSignin(config.adminEmail, config.adminPassword);
  const snapshot = waitForCouponIssueSettlement(
    adminToken.accessToken,
    data.couponId,
    {
      totalQuantity: data.totalQuantity,
      expectedIssuedCount: data.expectedSuccessCount,
      expectedRemainingQuantity: data.expectedRemainingQuantity,
    },
  );
  const issuedCount = snapshot.issuedCount;
  const remainingQuantity = snapshot.remainingQuantity;
  const integrityOk = snapshot.integrityOk;
  const expectedResultOk = snapshot.expectedResultOk;

  restaurantIssueBurstFinalIssuedCount.add(issuedCount);
  restaurantIssueBurstFinalRemainingQuantity.add(remainingQuantity);
  restaurantIssueBurstIntegrityOk.add(integrityOk);
  restaurantIssueBurstExpectedResultOk.add(expectedResultOk);

  check(
    {
      integrityOk,
      expectedResultOk,
    },
    {
      'restaurant_issue_burst stock invariant': (result) => result.integrityOk,
      'restaurant_issue_burst expected issue result': (result) => result.expectedResultOk,
    },
  );
}

export function handleSummary(data) {
  return buildSummary('restaurant-issue-burst', data);
}
