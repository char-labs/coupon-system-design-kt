package com.conpon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CouponServerApplication

fun main(args: Array<String>) {
    runApplication<CouponServerApplication>(*args)
}
