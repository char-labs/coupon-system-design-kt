package com.coupon.support.testing

import com.coupon.coupon.command.CouponCommand
import com.coupon.enums.coupon.CouponTrafficPolicy
import com.coupon.enums.coupon.CouponType
import com.coupon.user.command.UserCommand
import java.time.LocalDateTime
import java.util.UUID

internal object CouponWorkerFixtures {
    fun userCreateCommand(
        index: Int = 1,
        email: String = "worker-user-$index-${UUID.randomUUID()}@coupon.local",
    ): UserCommand.Create =
        UserCommand.Create(
            name = "worker-user-$index",
            email = email,
            password = "password",
        )

    fun couponCreateCommand(
        totalQuantity: Long = 1L,
        trafficPolicy: CouponTrafficPolicy = CouponTrafficPolicy.HOT_FCFS_ASYNC,
        referenceTime: LocalDateTime = LocalDateTime.now(),
    ): CouponCommand.Create =
        CouponCommand.Create(
            name = "worker-coupon-${UUID.randomUUID().toString().take(8)}",
            couponType = CouponType.FIXED,
            discountAmount = 1_000L,
            maxDiscountAmount = null,
            minOrderAmount = null,
            totalQuantity = totalQuantity,
            trafficPolicy = trafficPolicy,
            availableAt = referenceTime.minusMinutes(5),
            endAt = referenceTime.plusHours(1),
        )
}
