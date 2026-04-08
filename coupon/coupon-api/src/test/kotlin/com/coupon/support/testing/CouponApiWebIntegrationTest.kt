package com.coupon.support.testing

import com.coupon.CouponServerApplication
import io.kotest.core.extensions.ApplyExtension
import io.kotest.extensions.spring.SpringRootTestExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ApplyExtension(SpringRootTestExtension::class)
@SpringBootTest(
    classes = [CouponServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.main.web-application-type=servlet"],
)
@ActiveProfiles("test", "api-int")
@AutoConfigureMockMvc
@Import(CouponApiTestSupportConfig::class, CouponApiConcurrencyTestConfig::class)
annotation class CouponApiWebIntegrationTest
