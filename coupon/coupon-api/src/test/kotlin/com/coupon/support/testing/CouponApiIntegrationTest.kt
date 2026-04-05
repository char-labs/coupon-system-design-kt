package com.coupon.support.testing

import com.coupon.CouponServerApplication
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringRootTestExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ApplyExtension(SpringRootTestExtension::class)
@SpringBootTest(
    classes = [CouponServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("test", "api-int")
@Import(CouponApiTestSupportConfig::class)
annotation class CouponApiIntegrationTest
