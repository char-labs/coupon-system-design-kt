import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';
import { config } from './config.js';
import { maybeRecordSlowRequestSample } from './slow-requests.js';

const readinessResponseCallback = http.expectedStatuses(200, 404, 503);

function makeUrl(path) {
  return `${config.baseUrl}${path}`;
}

function parseJson(response, label) {
  try {
    return response.json();
  } catch (error) {
    fail(`${label}: failed to parse JSON (${error})`);
  }
}

function parseJsonIfPresent(response) {
  if (!response.body) {
    return null;
  }

  try {
    return response.json();
  } catch (error) {
    return null;
  }
}

function renderBody(response) {
  const body = response.body || '<empty>';
  if (body.length <= 240) {
    return body;
  }

  return `${body.slice(0, 240)}...`;
}

function request(method, path, payload, params = {}, label = path) {
  const {
    headers: customHeaders = {},
    tags: customTags = {},
    slowRequestSample = null,
    ...restParams
  } = params;

  const headers = {
    ...(payload !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...customHeaders,
  };

  const requestParams = {
    ...restParams,
    tags: {
      name: label,
      ...customTags,
    },
    headers,
  };

  if (method === 'GET') {
    return {
      response: http.get(makeUrl(path), requestParams),
      slowRequestSample,
    };
  }

  const requestBody =
    typeof payload === 'string'
      ? payload
      : JSON.stringify(payload);

  return {
    response: http.post(makeUrl(path), requestBody, requestParams),
    slowRequestSample,
  };
}

function extractNumericField(body, field) {
  if (!body) {
    return null;
  }

  const match = body.match(new RegExp(`"${field}"\\s*:\\s*(\\d+)`));
  return match ? match[1] : null;
}

export function unwrapData(response, label, expectedStatus) {
  const body = parseJson(response, label);
  const isExpectedStatus = response.status === expectedStatus;
  const isSuccessEnvelope = body && body.success === true;

  check(response, {
    [`${label} status ${expectedStatus}`]: () => isExpectedStatus,
    [`${label} success envelope`]: () => isSuccessEnvelope,
  });

  if (!isExpectedStatus || !isSuccessEnvelope) {
    fail(
      `${label}: unexpected response status=${response.status} body=${response.body}`,
    );
  }

  return body.data;
}

export function post(path, payload, params = {}, expectedStatus = 200, label = path) {
  const { response, slowRequestSample } = request('POST', path, payload, params, label);
  maybeRecordSlowRequestSample(response, slowRequestSample);

  return unwrapData(response, label, expectedStatus);
}

export function postWithNumericFields(
  path,
  payload,
  numericFields = [],
  params = {},
  expectedStatus = 200,
  label = path,
) {
  const { response, slowRequestSample } = request('POST', path, payload, params, label);
  maybeRecordSlowRequestSample(response, slowRequestSample);
  const data = unwrapData(response, label, expectedStatus);

  for (const field of numericFields) {
    const preservedValue = extractNumericField(response.body, field);
    if (preservedValue !== null && data && Object.prototype.hasOwnProperty.call(data, field)) {
      data[field] = preservedValue;
    }
  }

  return data;
}

export function postDataWithStatuses(
  path,
  payload,
  params = {},
  expectedStatuses = [200],
  label = path,
) {
  const { response, slowRequestSample } = request('POST', path, payload, params, label);
  maybeRecordSlowRequestSample(response, slowRequestSample);
  const body = parseJson(response, label);
  const isExpectedStatus = expectedStatuses.includes(response.status);
  const isSuccessEnvelope = body && body.success === true;

  check(response, {
    [`${label} status ${expectedStatuses.join('/')}`]: () => isExpectedStatus,
    [`${label} success envelope`]: () => isSuccessEnvelope,
  });

  if (!isExpectedStatus || !isSuccessEnvelope) {
    fail(
      `${label}: unexpected response status=${response.status} body=${response.body}`,
    );
  }

  return {
    data: body.data,
    status: response.status,
  };
}

export function postText(path, payload, params = {}, expectedStatus = 200, label = path) {
  return postTextWithStatuses(path, payload, params, [expectedStatus], label).body;
}

export function postTextWithStatuses(path, payload, params = {}, expectedStatuses = [200], label = path) {
  const { response, slowRequestSample } = request('POST', path, payload, params, label);
  maybeRecordSlowRequestSample(response, slowRequestSample);

  const isExpectedStatus = expectedStatuses.includes(response.status);
  check(response, {
    [`${label} status ${expectedStatuses.join('/')}`]: () => isExpectedStatus,
  });

  if (!isExpectedStatus) {
    fail(
      `${label}: unexpected response status=${response.status} body=${response.body}`,
    );
  }

  return {
    body: `${response.body || ''}`.trim(),
    status: response.status,
  };
}

export function postVoid(path, payload, params = {}, expectedStatus = 200, label = path) {
  const { response, slowRequestSample } = request('POST', path, payload, params, label);
  maybeRecordSlowRequestSample(response, slowRequestSample);
  const body = parseJsonIfPresent(response);
  const isExpectedStatus = response.status === expectedStatus;
  const isSuccessEnvelope = body == null || body.success === true;

  check(response, {
    [`${label} status ${expectedStatus}`]: () => isExpectedStatus,
    [`${label} success envelope or empty body`]: () => isSuccessEnvelope,
  });

  if (!isExpectedStatus || !isSuccessEnvelope) {
    fail(
      `${label}: unexpected response status=${response.status} body=${response.body}`,
    );
  }

  return body ? body.data : null;
}

export function get(path, params = {}, expectedStatus = 200, label = path) {
  const { response, slowRequestSample } = request('GET', path, undefined, params, label);
  maybeRecordSlowRequestSample(response, slowRequestSample);

  return unwrapData(response, label, expectedStatus);
}

export function authHeaders(accessToken) {
  return {
    Authorization: `Bearer ${accessToken}`,
  };
}

export function signupUser(email, password, name = 'K6 User') {
  return post(
    '/signup',
    { name, email, password },
    {},
    201,
    'signup',
  );
}

export function signin(email, password) {
  return post(
    '/signin',
    { email, password },
    {},
    200,
    'signin',
  );
}

export function createUserSession(email, name, password = config.testUserPassword) {
  signupUser(email, password, name);
  const token = signin(email, password);
  return token.accessToken;
}

function waitUntil(operation, label) {
  const deadline = Date.now() + config.startupTimeoutSeconds * 1000;
  let lastStatus = 'n/a';
  let lastBody = '<empty>';

  while (Date.now() < deadline) {
    const result = operation();
    lastStatus = result.response.status;
    lastBody = renderBody(result.response);

    if (result.ready) {
      return result.data;
    }

    sleep(config.startupPollIntervalSeconds);
  }

  fail(
    `${label}: timed out after ${config.startupTimeoutSeconds}s status=${lastStatus} body=${lastBody}`,
  );
}

export function waitForAppReady() {
  return waitUntil(() => {
    const { response } = request(
      'GET',
      '/actuator/health',
      undefined,
      {
        responseCallback: readinessResponseCallback,
      },
      'app_ready',
    );
    const body = parseJsonIfPresent(response);

    return {
      ready: response.status === 200 && body && body.status === 'UP',
      response,
    };
  }, 'app_ready');
}

export function waitForAdminSignin(email, password) {
  waitForAppReady();

  return waitUntil(() => {
    const signin = request(
      'POST',
      '/signin',
      { email, password },
      {
        responseCallback: http.expectedStatuses(200, 400, 401, 404, 503),
      },
      'admin_signin_ready',
    );
    const body = parseJsonIfPresent(signin.response);
    const token = body && body.success === true ? body.data : null;

    return {
      ready: signin.response.status === 200 && token && token.accessToken,
      data: token,
      response: signin.response,
    };
  }, 'admin_signin_ready');
}

export function prepareUserSessions(
  count,
  {
    prefix = 'k6-user',
    password = config.testUserPassword,
    batchSize = config.userSetupBatchSize,
    runId = Date.now(),
  } = {},
) {
  if (count < 0) {
    fail(`prepare_user_sessions: count must be >= 0 (received=${count})`);
  }

  if (count === 0) {
    return {
      accessTokens: [],
    };
  }

  const accessTokens = [];
  const normalizedBatchSize = Math.max(1, batchSize);

  for (let startIndex = 0; startIndex < count; startIndex += normalizedBatchSize) {
    const endIndex = Math.min(startIndex + normalizedBatchSize, count);
    const batchRequests = [];
    const batchMeta = [];

    for (let index = startIndex; index < endIndex; index += 1) {
      const email = `${prefix}+${runId}-${index}@coupon.local`;
      const name = `${prefix} user ${index + 1}`;

      batchMeta.push({ email });
      batchRequests.push([
        'POST',
        makeUrl('/signup'),
        JSON.stringify({ name, email, password }),
        {
          headers: {
            'Content-Type': 'application/json',
          },
          tags: {
            request_group: 'setup',
            request_name: 'signup_user',
          },
        },
      ]);
    }

    const responses = http.batch(batchRequests);
    responses.forEach((response, offset) => {
      const meta = batchMeta[offset];
      const body = parseJsonIfPresent(response);
      const token = body && body.success === true ? body.data : null;
      const isExpectedStatus = response.status === 201;

      check(response, {
        'prepare_user_sessions signup status 201': () => isExpectedStatus,
        'prepare_user_sessions signup success envelope': () => body && body.success === true,
      });

      if (!isExpectedStatus || !token || !token.accessToken) {
        fail(
          `prepare_user_sessions: signup failed for email=${meta.email} ` +
            `status=${response.status} body=${renderBody(response)}`,
        );
      }

      accessTokens.push(token.accessToken);
    });
  }

  return {
    accessTokens,
  };
}

export function createVuEmail(prefix = 'k6-user') {
  const vuId = exec.vu.idInTest || 0;
  const iteration = exec.scenario.iterationInTest || 0;
  return `${prefix}+${vuId}-${iteration}-${Date.now()}@coupon.local`;
}
