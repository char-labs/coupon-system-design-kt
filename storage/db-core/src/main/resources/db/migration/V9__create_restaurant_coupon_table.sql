CREATE TABLE t_restaurant_coupon (
    id              BIGINT       NOT NULL,
    restaurant_id   BIGINT       NOT NULL,
    coupon_id       BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    available_at    DATETIME(6)  NOT NULL,
    end_at          DATETIME(6)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NULL,
    deleted_at      DATETIME(6)  NULL,
    CONSTRAINT pk_restaurant_coupon PRIMARY KEY (id),
    CONSTRAINT uk_restaurant_coupon_rid_cid UNIQUE (restaurant_id, coupon_id),
    CONSTRAINT fk_restaurant_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES t_coupon (id)
);

CREATE INDEX idx_restaurant_coupon_restaurant_id ON t_restaurant_coupon (restaurant_id);
CREATE INDEX idx_restaurant_coupon_status ON t_restaurant_coupon (status);
