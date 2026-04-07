CREATE TABLE t_coupon_issue_request (
    id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    result_code VARCHAR(30) NULL,
    coupon_issue_id BIGINT NULL,
    failure_reason TEXT NULL,
    processed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_coupon_issue_request PRIMARY KEY (id),
    CONSTRAINT uk_coupon_issue_request_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_coupon_issue_request_coupon FOREIGN KEY (coupon_id) REFERENCES t_coupon (id),
    CONSTRAINT fk_coupon_issue_request_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_coupon_issue_request_coupon_issue FOREIGN KEY (coupon_issue_id) REFERENCES t_coupon_issue (id)
);

CREATE INDEX idx_coupon_issue_request_status_created_at
    ON t_coupon_issue_request (status, created_at);

CREATE INDEX idx_coupon_issue_request_coupon_id_created_at
    ON t_coupon_issue_request (coupon_id, created_at);

CREATE INDEX idx_coupon_issue_request_user_id_created_at
    ON t_coupon_issue_request (user_id, created_at);
