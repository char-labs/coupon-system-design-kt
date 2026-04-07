package com.coupon.coupon.activity

import com.coupon.coupon.activity.criteria.CouponActivityCriteria
import org.springframework.stereotype.Service

@Service
class CouponActivityService(
    private val couponActivityRepository: CouponActivityRepository,
) {
    fun recordIfAbsent(criteria: CouponActivityCriteria.Create): CouponActivity = couponActivityRepository.saveIfAbsent(criteria)
}
