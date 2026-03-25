CREATE TABLE t_user (
    id BIGINT NOT NULL,
    user_key VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_user PRIMARY KEY (id),
    CONSTRAINT uk_user_user_key UNIQUE (user_key),
    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE INDEX idx_user_user_key ON t_user (user_key);

CREATE TABLE t_coupon (
    id BIGINT NOT NULL,
    coupon_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    coupon_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    discount_amount BIGINT NOT NULL,
    max_discount_amount BIGINT NULL,
    min_order_amount BIGINT NULL,
    total_quantity BIGINT NOT NULL,
    remaining_quantity BIGINT NOT NULL,
    available_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_coupon PRIMARY KEY (id),
    CONSTRAINT uk_coupon_coupon_code UNIQUE (coupon_code)
);

CREATE TABLE t_coupon_issue (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    used_at DATETIME(6) NULL,
    canceled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_coupon_issue PRIMARY KEY (id),
    CONSTRAINT uk_coupon_issue_user_coupon UNIQUE (user_id, coupon_id),
    CONSTRAINT fk_coupon_issue_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_coupon_issue_coupon FOREIGN KEY (coupon_id) REFERENCES t_coupon (id)
);

CREATE TABLE t_authentication_history (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_key VARCHAR(64) NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    entity_status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT pk_authentication_history PRIMARY KEY (id)
);

CREATE INDEX idx_authentication_history_user_id ON t_authentication_history (user_id);
CREATE INDEX idx_authentication_history_user_key ON t_authentication_history (user_key);
