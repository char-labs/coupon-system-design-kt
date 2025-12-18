package com.coupon.storage.rdb.config

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EntityScan(
    basePackages = [
        "com.coupon.storage.rdb",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.coupon.storage.rdb",
    ],
)
class CoreJpaConfig
