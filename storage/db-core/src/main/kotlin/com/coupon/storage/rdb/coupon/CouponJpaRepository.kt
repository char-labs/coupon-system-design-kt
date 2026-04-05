package com.coupon.storage.rdb.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    fun findByCouponCode(couponCode: String): CouponEntity?

    fun existsByCouponCode(couponCode: String): Boolean

    fun findAllBy(pageable: Pageable): Page<CouponEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponEntity coupon
           set coupon.remainingQuantity = coupon.remainingQuantity - 1
         where coupon.id = :couponId
           and coupon.remainingQuantity > 0
        """,
    )
    fun decreaseQuantityIfAvailable(couponId: Long): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponEntity coupon
           set coupon.remainingQuantity = coupon.remainingQuantity + 1
         where coupon.id = :couponId
        """,
    )
    fun increaseQuantity(couponId: Long): Int
}
