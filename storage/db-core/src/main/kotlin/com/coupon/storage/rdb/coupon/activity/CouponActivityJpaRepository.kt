package com.coupon.storage.rdb.coupon.activity

import com.coupon.enums.coupon.CouponActivityType
import org.springframework.data.jpa.repository.JpaRepository

interface CouponActivityJpaRepository : JpaRepository<CouponActivityEntity, Long> {
    fun findByCouponIssueIdAndActivityType(
        couponIssueId: Long,
        activityType: CouponActivityType,
    ): CouponActivityEntity?
}
