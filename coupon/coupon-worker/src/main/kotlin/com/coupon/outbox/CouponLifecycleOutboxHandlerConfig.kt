package com.coupon.outbox

import com.coupon.coupon.event.CouponOutboxEventType
import com.coupon.enums.coupon.CouponActivityType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CouponLifecycleOutboxHandlerConfig(
    private val support: CouponLifecycleOutboxEventHandlerSupport,
) {
    @Bean
    fun couponIssuedOutboxEventHandler(): OutboxEventHandler =
        CouponLifecycleOutboxEventHandler(support, CouponOutboxEventType.COUPON_ISSUED, CouponActivityType.ISSUED)

    @Bean
    fun couponUsedOutboxEventHandler(): OutboxEventHandler =
        CouponLifecycleOutboxEventHandler(support, CouponOutboxEventType.COUPON_USED, CouponActivityType.USED)

    @Bean
    fun couponCanceledOutboxEventHandler(): OutboxEventHandler =
        CouponLifecycleOutboxEventHandler(support, CouponOutboxEventType.COUPON_CANCELED, CouponActivityType.CANCELED)
}
