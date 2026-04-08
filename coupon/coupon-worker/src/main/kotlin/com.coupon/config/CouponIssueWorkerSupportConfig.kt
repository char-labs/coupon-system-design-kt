package com.coupon.config

import com.coupon.coupon.CouponIssueEventPublisher
import com.coupon.coupon.CouponIssueMessage
import com.coupon.coupon.CouponIssuePublishReceipt
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CouponIssueWorkerSupportConfig {
    @Bean
    @ConditionalOnMissingBean(CouponIssueEventPublisher::class)
    fun workerCouponIssueEventPublisher(): CouponIssueEventPublisher =
        object : CouponIssueEventPublisher {
            override fun publish(message: CouponIssueMessage): CouponIssuePublishReceipt =
                throw UnsupportedOperationException("Coupon issue publisher is not available in worker runtime")
        }
}
