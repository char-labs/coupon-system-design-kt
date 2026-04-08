package com.coupon.support.testing

import com.coupon.CouponWorkerApplication
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
    classes = [CouponWorkerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("test", "worker-int")
@Import(CouponWorkerTestSupportConfig::class)
annotation class CouponWorkerIntegrationTest
