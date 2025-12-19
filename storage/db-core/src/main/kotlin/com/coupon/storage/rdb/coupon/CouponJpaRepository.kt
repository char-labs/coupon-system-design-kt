package com.coupon.storage.rdb.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    fun findByCouponCode(couponCode: String): CouponEntity?

    fun existsByCouponCode(couponCode: String): Boolean

    fun findAllBy(pageable: Pageable): Page<CouponEntity>
}
