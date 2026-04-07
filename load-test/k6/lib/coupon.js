import { check, fail, sleep } from 'k6';
import { authHeaders, get, post, postTextWithStatuses, postVoid, postWithNumericFields } from './api.js';
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
      tags: {
        request_group: 'setup',
        request_name: 'create_coupon',
      },
    },
    201,
    'create_coupon',
  );
}

export function acceptIssueCoupon(accessToken, couponId) {
  const response = postTextWithStatuses(
    '/coupon-issues',
    `{"couponId":${couponId}}`,
    {
      headers: authHeaders(accessToken),
      tags: {
        request_group: 'business',
        request_name: 'issue_coupon',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'issue_coupon',
      },
    },
    [200, 202],
    'issue_coupon',
  );

  return {
    result: response.body,
    status: response.status,
  };
}

export function requestIssueCoupon(accessToken, couponId) {
  return acceptIssueCoupon(accessToken, couponId);
}

export function issueCoupon(accessToken, couponId) {
  const issueResult = acceptIssueCoupon(accessToken, couponId);
  if (issueResult.result !== 'SUCCESS') {
    fail(`issue_coupon: expected SUCCESS but got ${issueResult.result}`);
  }

  return waitForIssuedCouponInMyCoupons(accessToken, couponId);
}

export function tryIssueCoupon(
  accessToken,
  couponId,
  {
    allowOutOfStock = false,
    allowRetryableLockFailure = false,
    label = 'issue_coupon',
  } = {},
) {
  const issueResult = acceptIssueCoupon(accessToken, couponId);

  if (issueResult.result === 'SUCCESS') {
    return {
      outcome: 'SUCCESS',
      status: issueResult.status,
    };
  }

  if (issueResult.result === 'SOLD_OUT') {
    return {
      outcome: allowOutOfStock ? 'OUT_OF_STOCK' : 'UNEXPECTED_ERROR',
      status: issueResult.status,
      errorClassName: 'SOLD_OUT',
      message: 'SOLD_OUT',
    };
  }

  if (issueResult.result === 'DUPLICATE') {
    return {
      outcome: 'UNEXPECTED_ERROR',
      status: issueResult.status,
      errorClassName: 'DUPLICATE',
      message: 'DUPLICATE',
    };
  }

  if (allowRetryableLockFailure) {
    return {
      outcome: 'UNEXPECTED_ERROR',
      status: 500,
      errorClassName: issueResult.result || 'UNKNOWN_ERROR',
      message: issueResult.result || '<empty>',
    };
  }

  return {
    outcome: 'UNEXPECTED_ERROR',
    status: 500,
    errorClassName: issueResult.result || 'UNKNOWN_ERROR',
    message: issueResult.result || '<empty>',
  };
}

export function activateCoupon(accessToken, couponId) {
  return postVoid(
    `/coupons/${couponId}/activate`,
    {},
    {
      headers: authHeaders(accessToken),
      tags: {
        request_group: 'setup',
        request_name: 'activate_coupon',
      },
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
      tags: {
        request_group: 'business',
        request_name: 'use_coupon',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'use_coupon',
      },
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
      tags: {
        request_group: 'business',
        request_name: 'cancel_coupon',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'cancel_coupon',
      },
    },
    200,
    'cancel_coupon',
  );
}

export function getMyCoupons(accessToken, size = 20) {
  return get(
    `/coupon-issues/my?page=0&size=${size}`,
    {
      headers: authHeaders(accessToken),
      tags: {
        request_group: 'business',
        request_name: 'get_my_coupons',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'get_my_coupons',
      },
    },
    200,
    'get_my_coupons',
  );
}

function waitForIssuedCouponInMyCoupons(
  accessToken,
  couponId,
  {
    timeoutSeconds = config.issuePollTimeoutSeconds,
    pollIntervalMs = config.issuePollIntervalMs,
  } = {},
) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  const expectedCouponId = `${couponId}`;

  while (Date.now() < deadline) {
    const page = getMyCoupons(accessToken, 100);
    const issue = (page.content || []).find((item) => `${item.couponId}` === expectedCouponId);
    if (issue) {
      return issue;
    }

    sleep(Math.max(pollIntervalMs, 0) / 1000);
  }

  fail(`coupon issue for couponId=${couponId} did not appear in my coupons within ${timeoutSeconds}s`);
}

export function getCoupon(couponId, accessToken = null) {
  return get(
    `/coupons/${couponId}`,
    accessToken
      ? {
          headers: authHeaders(accessToken),
          tags: {
            request_group: 'business',
            request_name: 'get_coupon',
          },
          slowRequestSample: {
            requestGroup: 'business',
            requestName: 'get_coupon',
          },
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
      tags: {
        request_group: 'business',
        request_name: 'get_coupon_issue_page',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'get_coupon_issue_page',
      },
    },
    200,
    'get_coupon_issue_page',
  );
}
