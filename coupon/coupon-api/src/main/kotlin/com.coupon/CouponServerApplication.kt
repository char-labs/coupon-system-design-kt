package com.coupon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class CouponServerApplication

fun main(args: Array<String>) {
    runApplication<CouponServerApplication>(*args)
}
