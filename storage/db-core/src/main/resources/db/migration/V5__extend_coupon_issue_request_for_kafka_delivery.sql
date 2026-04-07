ALTER TABLE t_coupon_issue_request
    ADD COLUMN enqueued_at DATETIME(6) NULL;

ALTER TABLE t_coupon_issue_request
    ADD COLUMN processing_started_at DATETIME(6) NULL;

ALTER TABLE t_coupon_issue_request
    ADD COLUMN delivery_attempt_count INT NOT NULL DEFAULT 0;

ALTER TABLE t_coupon_issue_request
    ADD COLUMN last_delivery_error TEXT NULL;

CREATE INDEX idx_coupon_issue_request_status_updated_at
    ON t_coupon_issue_request (status, updated_at, created_at);
