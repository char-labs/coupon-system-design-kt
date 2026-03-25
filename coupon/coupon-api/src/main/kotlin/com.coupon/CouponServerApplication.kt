package com.coupon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

@ConfigurationPropertiesScan
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class]) // TODO: Spring Security 적용 후 제거
class CouponServerApplication

fun main(args: Array<String>) {
    runApplication<CouponServerApplication>(*args)
}
