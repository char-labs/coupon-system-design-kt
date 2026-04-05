package com.coupon.coupon

import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page

interface CouponRepository {
    fun save(criteria: CouponCriteria.Create): Coupon

    fun findById(couponId: Long): Coupon

    fun findDetailById(couponId: Long): CouponDetail

    fun findByCode(couponCode: String): Coupon?

    fun existsByCode(couponCode: String): Boolean

    fun findAllBy(request: OffsetPageRequest): Page<CouponDetail>

    fun update(
        couponId: Long,
        criteria: CouponCriteria.Update,
    ): CouponDetail

    fun delete(couponId: Long)

    fun activate(couponId: Long)

    fun deactivate(couponId: Long)

    fun decreaseQuantityIfAvailable(couponId: Long): Boolean

    fun increaseQuantity(couponId: Long)
}
