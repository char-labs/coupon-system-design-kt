package com.coupon.support.testing

import com.coupon.CouponWorkerApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@SpringBootTest(
    classes = [CouponWorkerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("test", "worker-int")
@Import(CouponWorkerTestSupportConfig::class)
annotation class CouponWorkerIntegrationTest
