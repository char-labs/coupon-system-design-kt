package com.coupon.coupon.fixture

import com.coupon.coupon.CouponDetail
import com.coupon.enums.coupon.CouponStatus
import java.time.LocalDateTime

internal object CouponAvailabilityFixtures {
    fun inactive(base: CouponDetail = FixedCouponFixtures.standard()): CouponDetail = base.copy(status = CouponStatus.INACTIVE)

    fun outOfStock(base: CouponDetail = FixedCouponFixtures.standard()): CouponDetail =
        base.copy(
            totalQuantity = base.totalQuantity.coerceAtLeast(1L),
            remainingQuantity = 0L,
        )

    fun minimumOrderRequired(
        base: CouponDetail = FixedCouponFixtures.standard(),
        minOrderAmount: Long = 30_000L,
    ): CouponDetail = base.copy(minOrderAmount = minOrderAmount)

    fun future(
        base: CouponDetail = FixedCouponFixtures.standard(),
        referenceTime: LocalDateTime = LocalDateTime.now(),
    ): CouponDetail =
        base.copy(
            availableAt = referenceTime.plusDays(1),
            endAt = referenceTime.plusDays(2),
        )

    fun expired(
        base: CouponDetail = FixedCouponFixtures.standard(),
        referenceTime: LocalDateTime = LocalDateTime.now(),
    ): CouponDetail =
        base.copy(
            availableAt = referenceTime.minusDays(2),
            endAt = referenceTime.minusDays(1),
        )
}
