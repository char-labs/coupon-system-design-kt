package com.coupon.coupon.activity

import com.coupon.coupon.activity.criteria.CouponActivityCriteria
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponActivityService(
    private val couponActivityRepository: CouponActivityRepository,
) {
    @Transactional
    fun recordIfAbsent(criteria: CouponActivityCriteria.Create): CouponActivity = couponActivityRepository.saveIfAbsent(criteria)
}
