package com.coupon.coupon.fixture

import com.coupon.coupon.command.CouponCommand
import com.coupon.enums.coupon.CouponType
import java.time.LocalDateTime

internal object CouponCommandFixtures {
    fun create(
        name: String = "테스트 쿠폰",
        couponType: CouponType = CouponType.FIXED,
        discountAmount: Long = 5_000L,
        maxDiscountAmount: Long? = null,
        minOrderAmount: Long? = null,
        totalQuantity: Long = 10L,
        referenceTime: LocalDateTime = LocalDateTime.now(),
        availableAt: LocalDateTime = referenceTime.minusDays(1),
        endAt: LocalDateTime = referenceTime.plusDays(1),
    ) = CouponCommand.Create(
        name = name,
        couponType = couponType,
        discountAmount = discountAmount,
        maxDiscountAmount = maxDiscountAmount,
        minOrderAmount = minOrderAmount,
        totalQuantity = totalQuantity,
        availableAt = availableAt,
        endAt = endAt,
    )

    fun update(
        name: String? = "수정된 쿠폰",
        discountAmount: Long? = 7_000L,
        maxDiscountAmount: Long? = 10_000L,
        minOrderAmount: Long? = 30_000L,
        referenceTime: LocalDateTime = LocalDateTime.now(),
        availableAt: LocalDateTime? = referenceTime.minusHours(1),
        endAt: LocalDateTime? = referenceTime.plusDays(7),
    ) = CouponCommand.Update(
        name = name,
        discountAmount = discountAmount,
        maxDiscountAmount = maxDiscountAmount,
        minOrderAmount = minOrderAmount,
        availableAt = availableAt,
        endAt = endAt,
    )
}
