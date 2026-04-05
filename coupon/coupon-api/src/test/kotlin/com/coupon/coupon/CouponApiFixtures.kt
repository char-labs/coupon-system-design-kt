package com.coupon.coupon

import com.appmattus.kotlinfixture.Fixture
import com.appmattus.kotlinfixture.kotlinFixture
import com.coupon.coupon.command.CouponCommand
import com.coupon.enums.coupon.CouponType
import com.coupon.user.command.UserCommand
import java.time.LocalDateTime
import java.util.UUID

internal object CouponApiFixtures {
    private val fixture: Fixture = kotlinFixture()

    fun couponCreateCommand(
        totalQuantity: Long = 1L,
        couponType: CouponType = CouponType.FIXED,
        discountAmount: Long = 1_000L,
        maxDiscountAmount: Long? = null,
        minOrderAmount: Long? = null,
        referenceTime: LocalDateTime = LocalDateTime.now(),
        availableAt: LocalDateTime = referenceTime.minusMinutes(5),
        endAt: LocalDateTime = referenceTime.plusHours(1),
    ): CouponCommand.Create =
        fixture<CouponCommand.Create> {
            property(CouponCommand.Create::couponType) { couponType }
            property(CouponCommand.Create::discountAmount) { discountAmount }
            property(CouponCommand.Create::maxDiscountAmount) { maxDiscountAmount }
            property(CouponCommand.Create::minOrderAmount) { minOrderAmount }
            property(CouponCommand.Create::totalQuantity) { totalQuantity }
            property(CouponCommand.Create::availableAt) { availableAt }
            property(CouponCommand.Create::endAt) { endAt }
        }

    fun userCreateCommand(
        index: Int,
        email: String = "user-$index-${UUID.randomUUID()}@coupon.local",
        password: String = "password",
    ): UserCommand.Create =
        fixture<UserCommand.Create> {
            property(UserCommand.Create::email) { email }
            property(UserCommand.Create::password) { password }
        }
}
