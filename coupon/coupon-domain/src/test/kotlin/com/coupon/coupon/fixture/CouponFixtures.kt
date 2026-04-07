package com.coupon.coupon.fixture

import com.coupon.coupon.Coupon
import com.coupon.enums.coupon.CouponStatus
import com.coupon.enums.coupon.CouponTrafficPolicy
import com.coupon.enums.coupon.CouponType

internal object CouponFixtures {
    fun standard(
        id: Long = 1L,
        code: String = "CP-$id",
        name: String = "쿠폰-$id",
        type: CouponType = CouponType.FIXED,
        status: CouponStatus = CouponStatus.ACTIVE,
        trafficPolicy: CouponTrafficPolicy = CouponTrafficPolicy.HOT_FCFS_ASYNC,
    ) = Coupon(
        id = id,
        code = code,
        name = name,
        type = type,
        status = status,
        trafficPolicy = trafficPolicy,
    )
}
