package com.coupon.coupon.activity

import com.coupon.coupon.activity.criteria.CouponActivityCriteria

interface CouponActivityRepository {
    fun saveIfAbsent(criteria: CouponActivityCriteria.Create): CouponActivity
}
