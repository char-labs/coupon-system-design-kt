package com.coupon.coupon

import com.coupon.enums.coupon.CouponIssueResult
import java.time.Duration

interface CouponIssueStateRepository {
    fun reserve(
        couponId: Long,
        userId: Long,
        totalQuantity: Long,
        ttl: Duration,
    ): CouponIssueResult

    fun release(
        couponId: Long,
        userId: Long,
    )

    fun releaseStockSlot(couponId: Long)

    fun rebuild(
        couponId: Long,
        occupiedCount: Long,
        userIds: Set<Long>,
        ttl: Duration,
    )

    fun hasState(couponId: Long): Boolean

    fun clear(couponId: Long)
}
