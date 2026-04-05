package com.coupon.coupon.fixture

import com.coupon.coupon.CouponDetail
import com.coupon.enums.coupon.CouponType
import java.time.LocalDateTime

internal object PercentageCouponFixtures {
    fun standard(
        id: Long = 1L,
        discountRate: Long = 20L,
        maxDiscountAmount: Long? = null,
        minOrderAmount: Long? = null,
        totalQuantity: Long = 10L,
        remainingQuantity: Long = totalQuantity,
        referenceTime: LocalDateTime = LocalDateTime.now(),
    ): CouponDetail =
        CouponDetailFixtureFactory.build(
            id = id,
            code = "RATE-$id",
            name = "정률 쿠폰 $id",
            type = CouponType.PERCENTAGE,
            discountAmount = discountRate,
            maxDiscountAmount = maxDiscountAmount,
            minOrderAmount = minOrderAmount,
            totalQuantity = totalQuantity,
            remainingQuantity = remainingQuantity,
            referenceTime = referenceTime,
        )

    fun capped(
        id: Long = 1L,
        discountRate: Long = 30L,
        maxDiscountAmount: Long = 10_000L,
        referenceTime: LocalDateTime = LocalDateTime.now(),
    ): CouponDetail =
        standard(
            id = id,
            discountRate = discountRate,
            maxDiscountAmount = maxDiscountAmount,
            referenceTime = referenceTime,
        )
}
