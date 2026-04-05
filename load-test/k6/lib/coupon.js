import http from 'k6/http';
import { check } from 'k6';
import { authHeaders, get, post, postVoid, postWithNumericFields } from './api.js';
import { config } from './config.js';

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
  const availableAt = new Date(now.getTime() - 24 * 60 * 60 * 1000);
  const endAt = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);

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
  return postWithNumericFields(
    '/coupons',
    payload,
    ['id'],
    {
      headers: authHeaders(accessToken),
    },
    201,
    'create_coupon',
  );
}

export function issueCoupon(accessToken, couponId) {
  return postWithNumericFields(
    '/coupon-issues',
    `{"couponId":${couponId}}`,
    ['id'],
    {
      headers: authHeaders(accessToken),
    },
    201,
    'issue_coupon',
  );
}

export function tryIssueCoupon(
  accessToken,
  couponId,
  {
    allowOutOfStock = false,
    label = 'issue_coupon',
  } = {},
) {
  const expectedStatuses = allowOutOfStock ? [201, 400] : [201];
  const response = http.post(
    `${config.baseUrl}/coupon-issues`,
    JSON.stringify({ couponId }),
    {
      headers: {
        ...authHeaders(accessToken),
        'Content-Type': 'application/json',
      },
      tags: { name: label },
      responseCallback: http.expectedStatuses(...expectedStatuses),
    },
  );

  let body = null;

  try {
    body = response.json();
  } catch (error) {
    body = null;
  }

  const hasEnvelope = body && typeof body.success === 'boolean';
  check(response, {
    [`${label} response envelope`]: () => hasEnvelope,
  });

  if (!hasEnvelope) {
    return {
      outcome: 'UNEXPECTED_RESPONSE',
      status: response.status,
      errorClassName: 'INVALID_RESPONSE_ENVELOPE',
      message: response.body || '<empty>',
    };
  }

  if (response.status === 201 && body.success === true) {
    return {
      outcome: 'SUCCESS',
      status: response.status,
      issueId: body.data?.id ?? null,
    };
  }

  const errorClassName = body.data?.errorClassName || 'UNKNOWN_ERROR';
  const message = body.data?.message || response.body || '<empty>';

  if (response.status === 400 && errorClassName === 'COUPON_OUT_OF_STOCK') {
    return {
      outcome: 'OUT_OF_STOCK',
      status: response.status,
      errorClassName,
      message,
    };
  }

  return {
    outcome: 'UNEXPECTED_ERROR',
    status: response.status,
    errorClassName,
    message,
  };
}

export function activateCoupon(accessToken, couponId) {
  return postVoid(
    `/coupons/${couponId}/activate`,
    {},
    {
      headers: authHeaders(accessToken),
    },
    200,
    'activate_coupon',
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

export function getCoupon(couponId, accessToken = null) {
  return get(
    `/coupons/${couponId}`,
    accessToken
      ? {
          headers: authHeaders(accessToken),
        }
      : {},
    200,
    'get_coupon',
  );
}

export function getCouponIssuePage(accessToken, couponId, size = 1) {
  return get(
    `/coupons/${couponId}/coupon-issues?page=0&size=${size}`,
    {
      headers: authHeaders(accessToken),
    },
    200,
    'get_coupon_issue_page',
  );
}
