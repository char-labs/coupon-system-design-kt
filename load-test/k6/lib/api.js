import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';
import { config } from './config.js';

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
  const headers = {
    ...(payload !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...(params.headers || {}),
  };

  const requestParams = {
    tags: { name: label },
    ...params,
    headers,
  };

  if (method === 'GET') {
    return {
      response: http.get(makeUrl(path), requestParams),
    };
  }

  return {
    response: http.post(makeUrl(path), JSON.stringify(payload), requestParams),
  };
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
  const { response } = request('POST', path, payload, params, label);

  return unwrapData(response, label, expectedStatus);
}

export function get(path, params = {}, expectedStatus = 200, label = path) {
  const { response } = request('GET', path, undefined, params, label);

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
    const { response } = request(
      'POST',
      '/signin',
      { email, password },
      {
        responseCallback: readinessResponseCallback,
      },
      'admin_signin_ready',
    );
    const body = parseJsonIfPresent(response);
    const token = body && body.success === true ? body.data : null;

    return {
      ready: response.status === 200 && token && token.accessToken,
      data: token,
      response,
    };
  }, 'admin_signin_ready');
}

export function createVuEmail(prefix = 'k6-user') {
  const vuId = exec.vu.idInTest || 0;
  const iteration = exec.scenario.iterationInTest || 0;
  return `${prefix}+${vuId}-${iteration}-${Date.now()}@coupon.local`;
}
