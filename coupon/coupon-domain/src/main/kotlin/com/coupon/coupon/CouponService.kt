package com.coupon.coupon

import com.coupon.coupon.command.CouponCommand
import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val couponCodeGenerator: CouponCodeGenerator,
) {
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

    internal fun validateCouponAvailability(couponId: Long) {
        val coupon = couponRepository.findDetailById(couponId)

        if (coupon.status != com.coupon.enums.CouponStatus.ACTIVE) {
            throw ErrorException(ErrorType.COUPON_NOT_ACTIVE)
        }

        if (coupon.remainingQuantity <= 0) {
            throw ErrorException(ErrorType.COUPON_OUT_OF_STOCK)
        }

        val now = java.time.LocalDateTime.now()
        if (now.isBefore(coupon.availableAt) || now.isAfter(coupon.endAt)) {
            throw ErrorException(ErrorType.COUPON_EXPIRED)
        }
    }
}
