package com.coupon.support.testing

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CouponApiIntegrationTest
@Import(CouponApiConcurrencyTestConfig::class)
annotation class CouponApiConcurrencyTest
