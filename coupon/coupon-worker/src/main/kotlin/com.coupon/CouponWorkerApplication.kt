package com.coupon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
class CouponWorkerApplication

fun main(args: Array<String>) {
    runApplication<CouponWorkerApplication>(*args)
}
