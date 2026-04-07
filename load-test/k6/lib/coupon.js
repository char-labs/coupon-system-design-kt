import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { authHeaders, get, post, postVoid, postWithNumericFields } from './api.js';
import { config } from './config.js';
import { maybeRecordSlowRequestSample } from './slow-requests.js';

const terminalRequestStatuses = new Set(['SUCCEEDED', 'FAILED', 'DEAD']);

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

export function issueCoupon(accessToken, couponId) {
  return postWithNumericFields(
    '/coupon-issues',
    `{"couponId":${couponId}}`,
    ['id'],
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
    201,
    'issue_coupon',
  );
}

export function requestIssueCoupon(accessToken, couponId) {
  return postWithNumericFields(
    '/coupon-issue-requests',
    `{"couponId":${couponId}}`,
    ['id', 'couponId', 'userId', 'couponIssueId'],
    {
      headers: authHeaders(accessToken),
      tags: {
        request_group: 'business',
        request_name: 'issue_coupon_request',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'issue_coupon_request',
      },
    },
    202,
    'issue_coupon_request',
  );
}

export function requestIssueCouponLoadTest(couponId, userId) {
  return postWithNumericFields(
    `/load-test/coupons/${couponId}/issue-requests`,
    { userId },
    ['id', 'couponId', 'userId', 'couponIssueId'],
    {
      tags: {
        request_group: 'business',
        request_name: 'issue_coupon_request',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'issue_coupon_request',
      },
    },
    202,
    'issue_coupon_request_load_test',
  );
}

export function getCouponIssueRequest(accessToken, requestId) {
  return get(
    `/coupon-issue-requests/${requestId}`,
    {
      headers: authHeaders(accessToken),
      tags: {
        request_group: 'business',
        request_name: 'get_coupon_issue_request',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'get_coupon_issue_request',
      },
    },
    200,
    'get_coupon_issue_request',
  );
}

export function getCouponIssueRequestLoadTest(requestId) {
  return get(
    `/load-test/coupon-issue-requests/${requestId}`,
    {
      tags: {
        request_group: 'business',
        request_name: 'get_coupon_issue_request',
      },
      slowRequestSample: {
        requestGroup: 'business',
        requestName: 'get_coupon_issue_request',
      },
    },
    200,
    'get_coupon_issue_request_load_test',
  );
}

function waitForCouponIssueRequestTerminalInternal(
  fetchRequest,
  requestId,
  {
    timeoutSeconds = config.issueRequestPollTimeoutSeconds,
    pollIntervalMs = config.issueRequestPollIntervalMs,
  } = {},
) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  let latestRequest = null;

  while (Date.now() < deadline) {
    latestRequest = fetchRequest();
    if (terminalRequestStatuses.has(latestRequest.status)) {
      return latestRequest;
    }

    sleep(Math.max(pollIntervalMs, 0) / 1000);
  }

  fail(
    `coupon issue request ${requestId} did not reach terminal status within ${timeoutSeconds}s ` +
      `(lastStatus=${latestRequest?.status || 'UNKNOWN'}, lastResultCode=${latestRequest?.resultCode || 'n/a'})`,
  );
}

export function waitForCouponIssueRequestTerminal(
  accessToken,
  requestId,
  options = {},
) {
  return waitForCouponIssueRequestTerminalInternal(
    () => getCouponIssueRequest(accessToken, requestId),
    requestId,
    options,
  );
}

export function waitForCouponIssueRequestTerminalLoadTest(
  requestId,
  options = {},
) {
  return waitForCouponIssueRequestTerminalInternal(
    () => getCouponIssueRequestLoadTest(requestId),
    requestId,
    options,
  );
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
  const expectedStatuses = [201];
  if (allowOutOfStock) {
    expectedStatuses.push(400);
  }
  if (allowRetryableLockFailure) {
    expectedStatuses.push(429);
  }
  const response = http.post(
    `${config.baseUrl}/coupon-issues`,
    JSON.stringify({ couponId }),
    {
      headers: {
        ...authHeaders(accessToken),
        'Content-Type': 'application/json',
      },
      tags: {
        name: label,
        request_group: 'business',
        request_name: 'issue_coupon',
      },
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
  maybeRecordSlowRequestSample(response, {
    requestGroup: 'business',
    requestName: 'issue_coupon',
  });
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

  if (
    response.status === 429 &&
    errorClassName === 'LOCK_ACQUISITION_FAILED'
  ) {
    return {
      outcome: 'RETRYABLE_LOCK_FAILURE',
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

export function getMyCoupons(accessToken) {
  return get(
    '/coupon-issues/my?page=0&size=20',
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
