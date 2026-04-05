package com.coupon.coupon.fixture

import com.coupon.coupon.CouponDetail
import java.time.LocalDateTime

internal object FixedCouponFixtures {
    fun standard(
        id: Long = 1L,
        discountAmount: Long = 5_000L,
        minOrderAmount: Long? = null,
        totalQuantity: Long = 10L,
        remainingQuantity: Long = totalQuantity,
        referenceTime: LocalDateTime = LocalDateTime.now(),
    ): CouponDetail =
        CouponDetailFixtureFactory.build(
            id = id,
            code = "FIXED-$id",
            name = "정액 쿠폰 $id",
            discountAmount = discountAmount,
            minOrderAmount = minOrderAmount,
            totalQuantity = totalQuantity,
            remainingQuantity = remainingQuantity,
            referenceTime = referenceTime,
        )

    fun singleQuantity(
        id: Long = 1L,
        discountAmount: Long = 5_000L,
        referenceTime: LocalDateTime = LocalDateTime.now(),
    ): CouponDetail =
        standard(
            id = id,
            discountAmount = discountAmount,
            totalQuantity = 1L,
            remainingQuantity = 1L,
            referenceTime = referenceTime,
        )
}
