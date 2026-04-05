package com.coupon.coupon.fixture

import com.coupon.coupon.Coupon
import com.coupon.enums.coupon.CouponStatus
import com.coupon.enums.coupon.CouponType

internal object CouponFixtures {
    fun standard(
        id: Long = 1L,
        code: String = "CP-$id",
        name: String = "쿠폰-$id",
        type: CouponType = CouponType.FIXED,
        status: CouponStatus = CouponStatus.ACTIVE,
    ) = Coupon(
        id = id,
        code = code,
        name = name,
        type = type,
        status = status,
    )
}
