package com.coupon.coupon.fixture

import com.appmattus.kotlinfixture.Fixture
import com.appmattus.kotlinfixture.kotlinFixture
import com.coupon.coupon.CouponDetail
import com.coupon.enums.coupon.CouponStatus
import com.coupon.enums.coupon.CouponType
import java.time.LocalDateTime

internal object CouponDetailFixtureFactory {
    private val fixture: Fixture = kotlinFixture()

    fun build(
        id: Long = 1L,
        code: String = "CP-$id",
        name: String = "쿠폰-$id",
        status: CouponStatus = CouponStatus.ACTIVE,
        type: CouponType = CouponType.FIXED,
        discountAmount: Long = 5_000L,
        maxDiscountAmount: Long? = null,
        minOrderAmount: Long? = null,
        totalQuantity: Long = 10L,
        remainingQuantity: Long = totalQuantity,
        referenceTime: LocalDateTime = LocalDateTime.now(),
        availableAt: LocalDateTime = referenceTime.minusDays(30),
        endAt: LocalDateTime = referenceTime.plusDays(30),
        createdAt: LocalDateTime = referenceTime.minusDays(31),
        updatedAt: LocalDateTime? = referenceTime.minusDays(1),
    ): CouponDetail =
        fixture<CouponDetail> {
            property(CouponDetail::id) { id }
            property(CouponDetail::code) { code }
            property(CouponDetail::name) { name }
            property(CouponDetail::status) { status }
            property(CouponDetail::type) { type }
            property(CouponDetail::discountAmount) { discountAmount }
            property(CouponDetail::maxDiscountAmount) { maxDiscountAmount }
            property(CouponDetail::minOrderAmount) { minOrderAmount }
            property(CouponDetail::totalQuantity) { totalQuantity }
            property(CouponDetail::remainingQuantity) { remainingQuantity }
            property(CouponDetail::availableAt) { availableAt }
            property(CouponDetail::endAt) { endAt }
            property(CouponDetail::createdAt) { createdAt }
            property(CouponDetail::updatedAt) { updatedAt }
        }
}
