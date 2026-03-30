import { authHeaders, get, post } from './api.js';

function formatLocalDateTime(date) {
  const pad = (value) => `${value}`.padStart(2, '0');

  return [
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`,
  ].join('T');
}

export function buildCouponPayload({
  name,
  totalQuantity,
  discountAmount = 5000,
  minOrderAmount = 30000,
}) {
  const now = new Date();
  const availableAt = new Date(now.getTime() - 60 * 1000);
  const endAt = new Date(now.getTime() + 24 * 60 * 60 * 1000);

  return {
    name,
    couponType: 'FIXED',
    discountAmount,
    maxDiscountAmount: null,
    minOrderAmount,
    totalQuantity,
    availableAt: formatLocalDateTime(availableAt),
    endAt: formatLocalDateTime(endAt),
  };
}

export function createCoupon(accessToken, payload) {
  return post(
    '/coupons',
    payload,
    {
      headers: authHeaders(accessToken),
    },
    201,
    'create_coupon',
  );
}

export function issueCoupon(accessToken, couponId) {
  return post(
    '/coupon-issues',
    { couponId },
    {
      headers: authHeaders(accessToken),
    },
    201,
    'issue_coupon',
  );
}

export function useCoupon(accessToken, couponIssueId) {
  return post(
    `/coupon-issues/${couponIssueId}/use`,
    {},
    {
      headers: authHeaders(accessToken),
    },
    200,
    'use_coupon',
  );
}

export function cancelCoupon(accessToken, couponIssueId) {
  return post(
    `/coupon-issues/${couponIssueId}/cancel`,
    {},
    {
      headers: authHeaders(accessToken),
    },
    200,
    'cancel_coupon',
  );
}

export function getMyCoupons(accessToken) {
  return get(
    '/coupon-issues/my?page=0&size=20',
    {
      headers: authHeaders(accessToken),
    },
    200,
    'get_my_coupons',
  );
}
