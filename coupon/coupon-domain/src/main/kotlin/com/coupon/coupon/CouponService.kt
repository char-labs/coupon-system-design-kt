package com.coupon.coupon

import com.coupon.coupon.command.CouponCommand
import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val couponCodeGenerator: CouponCodeGenerator,
) {
    /**
     * Admin write path for coupon master data.
     * This API is fully synchronous and commits directly to the coupon table.
     */
    fun createCoupon(command: CouponCommand.Create): Coupon {
        val couponCode = couponCodeGenerator.generate()
        return couponRepository.save(
            CouponCriteria.Create.of(
                couponCode = couponCode,
                command = command,
            ),
        )
    }

    fun getCoupon(couponId: Long): CouponDetail = couponRepository.findDetailById(couponId)

    fun getCoupons(request: OffsetPageRequest): Page<CouponDetail> = couponRepository.findAllBy(request)

    fun modifyCoupon(
        couponId: Long,
        command: CouponCommand.Update,
    ): CouponDetail =
        Tx.writeable {
            couponRepository.update(couponId, CouponCriteria.Update.of(command))
        }

    fun activateCoupon(couponId: Long) =
        Tx.writeable {
            couponRepository.activate(couponId)
        }

    fun deactivateCoupon(couponId: Long) =
        Tx.writeable {
            couponRepository.deactivate(couponId)
        }

    fun deleteCoupon(couponId: Long) =
        Tx.writeable {
            couponRepository.delete(couponId)
        }
}
