export const config = {
  baseUrl: __ENV.BASE_URL || 'http://127.0.0.1:18080',
  adminName: __ENV.ADMIN_NAME || 'Load Test Admin',
  adminEmail: __ENV.ADMIN_EMAIL || 'loadtest-admin@coupon.local',
  adminPassword: __ENV.ADMIN_PASSWORD || 'admin1234!',
  testUserPassword: __ENV.TEST_USER_PASSWORD || 'coupon1234!',
  userSetupBatchSize: Number(__ENV.USER_SETUP_BATCH_SIZE || 100),
  startupTimeoutSeconds: Number(__ENV.STARTUP_TIMEOUT_SECONDS || 60),
  startupPollIntervalSeconds: Number(__ENV.STARTUP_POLL_INTERVAL_SECONDS || 1),
  smokeVus: Number(__ENV.SMOKE_VUS || 1),
  baselineVus: Number(__ENV.BASELINE_VUS || 20),
  baselineDuration: __ENV.BASELINE_DURATION || '10m',
  baselineSessionPoolSize: Number(__ENV.BASELINE_SESSION_POOL_SIZE || 100),
  baselineCouponPoolSize: Number(__ENV.BASELINE_COUPON_POOL_SIZE || 10),
  issueBurstVus: Number(__ENV.ISSUE_BURST_VUS || 1000),
  issueBurstStock: Number(
    __ENV.ISSUE_BURST_STOCK || __ENV.ISSUE_BURST_VUS || 1000,
  ),
  issueBurstMaxDuration: __ENV.ISSUE_BURST_MAX_DURATION || '5m',
  issueBurstSetupTimeout: __ENV.ISSUE_BURST_SETUP_TIMEOUT || '20m',
  slowRequestSampleLimit: Number(__ENV.SLOW_REQUEST_SAMPLE_LIMIT || 10),
  slowRequestPreviewMaxLength: Number(__ENV.SLOW_REQUEST_PREVIEW_MAX_LENGTH || 300),
  slowRequestSampleStdout: __ENV.SLOW_REQUEST_SAMPLE_STDOUT === '1',
  issueOverloadVus: Number(__ENV.ISSUE_OVERLOAD_VUS || 100),
  issueOverloadDuration: __ENV.ISSUE_OVERLOAD_DURATION || '10m',
  issueOverloadUserPoolSize: Number(
    __ENV.ISSUE_OVERLOAD_USER_POOL_SIZE ||
      Math.max(Number(__ENV.ISSUE_OVERLOAD_VUS || 100) * 2, 200),
  ),
  issueOverloadCouponPoolSize: Number(__ENV.ISSUE_OVERLOAD_COUPON_POOL_SIZE || 500),
  issueOverloadSetupTimeout: __ENV.ISSUE_OVERLOAD_SETUP_TIMEOUT || '10m',
  issuePollTimeoutSeconds: Number(
    __ENV.ISSUE_POLL_TIMEOUT_SECONDS || __ENV.ISSUE_REQUEST_POLL_TIMEOUT_SECONDS || 30,
  ),
  issuePollIntervalMs: Number(
    __ENV.ISSUE_POLL_INTERVAL_MS || __ENV.ISSUE_REQUEST_POLL_INTERVAL_MS || 500,
  ),
  issueRampGracefulStop:
    __ENV.ISSUE_RAMP_GRACEFUL_STOP || __ENV.ISSUE_REQUEST_RAMP_GRACEFUL_STOP || '30s',
  issueRampStock: Number(__ENV.ISSUE_RAMP_STOCK || __ENV.ISSUE_REQUEST_RAMP_STOCK || 10000000),
  issueRampUserPoolSize: Number(
    __ENV.ISSUE_RAMP_USER_POOL_SIZE ||
      __ENV.ISSUE_REQUEST_RAMP_USER_POOL_SIZE ||
      Math.max(
        Number(__ENV.ISSUE_RAMP_STAGE1_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE1_TARGET || 3000),
        Number(__ENV.ISSUE_RAMP_STAGE2_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE2_TARGET || 3000),
        Number(__ENV.ISSUE_RAMP_STAGE3_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE3_TARGET || 5000),
        Number(__ENV.ISSUE_RAMP_STAGE4_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE4_TARGET || 5000),
        Number(__ENV.ISSUE_RAMP_STAGE5_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE5_TARGET || 7000),
        Number(__ENV.ISSUE_RAMP_STAGE6_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE6_TARGET || 7000),
      ),
  ),
  issueRampCouponPoolSize: Number(
    __ENV.ISSUE_RAMP_COUPON_POOL_SIZE || __ENV.ISSUE_REQUEST_RAMP_COUPON_POOL_SIZE || 1000,
  ),
  issueRampStockPerCoupon: Number(
    __ENV.ISSUE_RAMP_STOCK_PER_COUPON || __ENV.ISSUE_REQUEST_RAMP_STOCK_PER_COUPON || 100000,
  ),
  issueRampStage1Duration: __ENV.ISSUE_RAMP_STAGE1_DURATION || __ENV.ISSUE_REQUEST_RAMP_STAGE1_DURATION || '3m',
  issueRampStage1Target: Number(__ENV.ISSUE_RAMP_STAGE1_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE1_TARGET || 3000),
  issueRampStage2Duration: __ENV.ISSUE_RAMP_STAGE2_DURATION || __ENV.ISSUE_REQUEST_RAMP_STAGE2_DURATION || '1m',
  issueRampStage2Target: Number(__ENV.ISSUE_RAMP_STAGE2_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE2_TARGET || 3000),
  issueRampStage3Duration: __ENV.ISSUE_RAMP_STAGE3_DURATION || __ENV.ISSUE_REQUEST_RAMP_STAGE3_DURATION || '2m',
  issueRampStage3Target: Number(__ENV.ISSUE_RAMP_STAGE3_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE3_TARGET || 5000),
  issueRampStage4Duration: __ENV.ISSUE_RAMP_STAGE4_DURATION || __ENV.ISSUE_REQUEST_RAMP_STAGE4_DURATION || '3m',
  issueRampStage4Target: Number(__ENV.ISSUE_RAMP_STAGE4_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE4_TARGET || 5000),
  issueRampStage5Duration: __ENV.ISSUE_RAMP_STAGE5_DURATION || __ENV.ISSUE_REQUEST_RAMP_STAGE5_DURATION || '2m',
  issueRampStage5Target: Number(__ENV.ISSUE_RAMP_STAGE5_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE5_TARGET || 7000),
  issueRampStage6Duration: __ENV.ISSUE_RAMP_STAGE6_DURATION || __ENV.ISSUE_REQUEST_RAMP_STAGE6_DURATION || '5m',
  issueRampStage6Target: Number(__ENV.ISSUE_RAMP_STAGE6_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE6_TARGET || 7000),
  issueRampStage7Duration: __ENV.ISSUE_RAMP_STAGE7_DURATION || __ENV.ISSUE_REQUEST_RAMP_STAGE7_DURATION || '3m',
  issueRampStage7Target: Number(__ENV.ISSUE_RAMP_STAGE7_TARGET || __ENV.ISSUE_REQUEST_RAMP_STAGE7_TARGET || 0),
  issueRealRampUserPoolSize: Number(
    __ENV.ISSUE_REAL_RAMP_USER_POOL_SIZE || __ENV.ISSUE_REQUEST_REAL_RAMP_USER_POOL_SIZE || 7000,
  ),
  issueRealRampCouponPoolSize: Number(
    __ENV.ISSUE_REAL_RAMP_COUPON_POOL_SIZE || __ENV.ISSUE_REQUEST_REAL_RAMP_COUPON_POOL_SIZE || 1000,
  ),
  issueRealRampStockPerCoupon: Number(
    __ENV.ISSUE_REAL_RAMP_STOCK_PER_COUPON || __ENV.ISSUE_REQUEST_REAL_RAMP_STOCK_PER_COUPON || 100000,
  ),
  issueRealRampSetupTimeout:
    __ENV.ISSUE_REAL_RAMP_SETUP_TIMEOUT || __ENV.ISSUE_REQUEST_REAL_RAMP_SETUP_TIMEOUT || '30m',
  contentionVus: Number(__ENV.CONTENTION_VUS || 100),
  contentionMaxDuration: __ENV.CONTENTION_MAX_DURATION || '2m',
  resultsDir: __ENV.RESULTS_DIR || 'load-test/k6/results',
};
