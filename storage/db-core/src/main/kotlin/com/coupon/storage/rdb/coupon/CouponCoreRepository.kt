package com.coupon.storage.rdb.coupon

import com.coupon.coupon.Coupon
import com.coupon.coupon.CouponDetail
import com.coupon.coupon.CouponRepository
import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.storage.rdb.support.findByIdOrElseThrow
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class CouponCoreRepository(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {
    override fun save(criteria: CouponCriteria.Create): Coupon =
        couponJpaRepository
            .save(CouponEntity(criteria))
            .toCoupon()

    override fun findById(couponId: Long): Coupon = couponJpaRepository.findByIdOrElseThrow(couponId).toCoupon()

    override fun findDetailById(couponId: Long): CouponDetail = couponJpaRepository.findByIdOrElseThrow(couponId).toCouponDetail()

    override fun findByCode(couponCode: String): Coupon? = couponJpaRepository.findByCouponCode(couponCode)?.toCoupon()

    override fun existsByCode(couponCode: String): Boolean = couponJpaRepository.existsByCouponCode(couponCode)

    override fun findAllBy(request: OffsetPageRequest): Page<CouponDetail> {
        val pageable = PageRequest.of(request.page, request.size)
        val result = couponJpaRepository.findAllBy(pageable)
        return Page(
            content = result.content.map { it.toCouponDetail() },
            totalCount = result.totalElements,
        )
    }

    override fun update(
        couponId: Long,
        criteria: CouponCriteria.Update,
    ): CouponDetail {
        val entity = couponJpaRepository.findByIdOrElseThrow(couponId)
        entity.update(criteria)
        return entity.toCouponDetail()
    }

    override fun delete(couponId: Long) {
        val entity = couponJpaRepository.findByIdOrElseThrow(couponId)
        entity.softDelete()
    }

    override fun activate(couponId: Long) {
        val entity = couponJpaRepository.findByIdOrElseThrow(couponId)
        entity.activate()
    }

    override fun deactivate(couponId: Long) {
        val entity = couponJpaRepository.findByIdOrElseThrow(couponId)
        entity.deactivate()
    }

    override fun decreaseQuantity(couponId: Long) {
        val entity = couponJpaRepository.findByIdOrElseThrow(couponId)
        entity.decreaseQuantity()
    }

    override fun increaseQuantity(couponId: Long) {
        val entity = couponJpaRepository.findByIdOrElseThrow(couponId)
        entity.increaseQuantity()
    }
}
