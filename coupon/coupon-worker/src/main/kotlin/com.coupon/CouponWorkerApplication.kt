package com.coupon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.scheduling.annotation.EnableScheduling

@EnableKafka
@EnableScheduling
@ConfigurationPropertiesScan(
    basePackages = [
        "com.coupon.config",
        "com.coupon.redis.config",
        "com.coupon.storage.rdb.config",
    ],
)
@SpringBootApplication
@ComponentScan(
    basePackages = [
        "com.coupon.coupon",
        "com.coupon.user",
        "com.coupon.support",
        "com.coupon.storage.rdb",
        "com.coupon.redis",
        "com.coupon.config",
        "com.coupon.outbox",
        "com.coupon.kafka",
        "com.coupon.health",
        "com.coupon.client",
    ],
)
class CouponWorkerApplication

fun main(args: Array<String>) {
    runApplication<CouponWorkerApplication>(*args)
}
