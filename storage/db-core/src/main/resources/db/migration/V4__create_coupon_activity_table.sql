CREATE TABLE t_coupon_activity (
    id BIGINT NOT NULL,
    coupon_issue_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(20) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_coupon_activity PRIMARY KEY (id),
    CONSTRAINT uk_coupon_activity_issue_type UNIQUE (coupon_issue_id, activity_type),
    CONSTRAINT fk_coupon_activity_coupon_issue FOREIGN KEY (coupon_issue_id) REFERENCES t_coupon_issue (id),
    CONSTRAINT fk_coupon_activity_coupon FOREIGN KEY (coupon_id) REFERENCES t_coupon (id),
    CONSTRAINT fk_coupon_activity_user FOREIGN KEY (user_id) REFERENCES t_user (id)
);

CREATE INDEX idx_coupon_activity_coupon_id_occurred_at
    ON t_coupon_activity (coupon_id, occurred_at);

CREATE INDEX idx_coupon_activity_user_id_occurred_at
    ON t_coupon_activity (user_id, occurred_at);
