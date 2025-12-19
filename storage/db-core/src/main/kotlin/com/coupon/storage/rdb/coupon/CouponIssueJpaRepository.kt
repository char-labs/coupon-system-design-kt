package com.coupon.storage.rdb.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CouponIssueJpaRepository : JpaRepository<CouponIssueEntity, Long> {
    fun findAllByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<CouponIssueEntity>

    fun findAllByCouponId(
        couponId: Long,
        pageable: Pageable,
    ): Page<CouponIssueEntity>

    fun existsByUserIdAndCouponId(
        userId: Long,
        couponId: Long,
    ): Boolean
}
