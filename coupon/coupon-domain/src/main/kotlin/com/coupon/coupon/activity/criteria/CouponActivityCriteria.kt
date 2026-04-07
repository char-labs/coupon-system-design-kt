package com.coupon.coupon.activity.criteria

import com.coupon.coupon.event.CouponLifecycleOutboxPayload
import com.coupon.enums.coupon.CouponActivityType
import java.time.LocalDateTime

sealed interface CouponActivityCriteria {
    data class Create(
        val couponIssueId: Long,
        val couponId: Long,
        val userId: Long,
        val activityType: CouponActivityType,
        val occurredAt: LocalDateTime,
    ) : CouponActivityCriteria {
        companion object {
            fun of(
                payload: CouponLifecycleOutboxPayload,
                activityType: CouponActivityType,
            ) = Create(
                couponIssueId = payload.couponIssueId,
                couponId = payload.couponId,
                userId = payload.userId,
                activityType = activityType,
                occurredAt = payload.occurredAt,
            )
        }
    }
}
