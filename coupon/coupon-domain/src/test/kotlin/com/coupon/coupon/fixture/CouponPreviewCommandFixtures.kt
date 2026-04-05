package com.coupon.coupon.fixture

import com.appmattus.kotlinfixture.Fixture
import com.appmattus.kotlinfixture.kotlinFixture
import com.coupon.coupon.command.CouponPreviewCommand

internal object CouponPreviewCommandFixtures {
    private val fixture: Fixture = kotlinFixture()

    fun standard(
        couponId: Long = 1L,
        userId: Long = 1L,
        orderAmount: Long = 50_000L,
    ): CouponPreviewCommand =
        fixture<CouponPreviewCommand> {
            property(CouponPreviewCommand::couponId) { couponId }
            property(CouponPreviewCommand::userId) { userId }
            property(CouponPreviewCommand::orderAmount) { orderAmount }
        }

    fun smallOrder(
        couponId: Long = 1L,
        userId: Long = 1L,
        orderAmount: Long = 4_000L,
    ): CouponPreviewCommand = standard(couponId = couponId, userId = userId, orderAmount = orderAmount)

    fun negativeOrder(
        couponId: Long = 1L,
        userId: Long = 1L,
    ): CouponPreviewCommand = standard(couponId = couponId, userId = userId, orderAmount = -1_000L)

    fun belowMinimumOrder(
        couponId: Long = 1L,
        userId: Long = 1L,
        minOrderAmount: Long = 30_000L,
    ): CouponPreviewCommand =
        standard(
            couponId = couponId,
            userId = userId,
            orderAmount = minOrderAmount - 1L,
        )

    fun exactMinimumOrder(
        couponId: Long = 1L,
        userId: Long = 1L,
        minOrderAmount: Long = 30_000L,
    ): CouponPreviewCommand =
        standard(
            couponId = couponId,
            userId = userId,
            orderAmount = minOrderAmount,
        )
}
