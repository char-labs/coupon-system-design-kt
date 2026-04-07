package com.coupon.coupon

import com.coupon.coupon.command.CouponCommand
import com.coupon.coupon.command.CouponPreviewCommand
import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.support.cache.Cache
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service
import tools.jackson.core.type.TypeReference

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val couponIssueRepository: CouponIssueRepository,
    private val couponCodeGenerator: CouponCodeGenerator,
    private val couponValidator: CouponValidator,
    private val couponEligibilityEvaluator: CouponEligibilityEvaluator,
    private val couponDiscountCalculator: CouponDiscountCalculator,
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

    fun getAvailableCouponForIssue(couponId: Long): CouponDetail =
        getCouponDetailForIssue(couponId).also(couponValidator::validateAvailability)

    fun validateAvailability(couponId: Long) {
        couponValidator.validateAvailability(getCouponDetailForIssue(couponId))
    }

    fun decreaseQuantityIfAvailable(couponId: Long): Boolean = couponRepository.decreaseQuantityIfAvailable(couponId)

    fun increaseQuantity(couponId: Long) {
        couponRepository.increaseQuantity(couponId)
    }

    fun preview(command: CouponPreviewCommand): CouponPreview {
        val coupon = couponRepository.findDetailById(command.couponId)
        val normalizedOrderAmount = command.orderAmount.coerceAtLeast(0)
        val alreadyIssued = couponIssueRepository.existsByUserIdAndCouponId(command.userId, command.couponId)
        val reason =
            couponEligibilityEvaluator.evaluate(
                coupon = coupon,
                orderAmount = normalizedOrderAmount,
                alreadyIssued = alreadyIssued,
            )

        return CouponPreview(
            couponId = coupon.id,
            couponCode = coupon.code,
            couponName = coupon.name,
            couponType = coupon.type,
            orderAmount = normalizedOrderAmount,
            applicable = reason == null,
            discountAmount =
                if (reason == null) {
                    couponDiscountCalculator.calculate(coupon, normalizedOrderAmount)
                } else {
                    0
                },
            reason = reason,
        )
    }

    fun modifyCoupon(
        couponId: Long,
        command: CouponCommand.Update,
    ): CouponDetail =
        Tx.writeable {
            couponRepository.update(couponId, CouponCriteria.Update.of(command)).also {
                clearIssueCouponDetailCache(couponId)
            }
        }

    fun activateCoupon(couponId: Long) =
        Tx.writeable {
            couponRepository.activate(couponId)
            clearIssueCouponDetailCache(couponId)
        }

    fun deactivateCoupon(couponId: Long) =
        Tx.writeable {
            couponRepository.deactivate(couponId)
            clearIssueCouponDetailCache(couponId)
        }

    fun deleteCoupon(couponId: Long) =
        Tx.writeable {
            couponRepository.delete(couponId)
            clearIssueCouponDetailCache(couponId)
        }

    private fun getCouponDetailForIssue(couponId: Long): CouponDetail =
        Cache.cache(
            ttl = ISSUE_COUPON_DETAIL_CACHE_TTL_MINUTES,
            key = issueCouponDetailCacheKey(couponId),
            typeReference = object : TypeReference<CouponDetail>() {},
        ) {
            couponRepository.findDetailById(couponId)
        }

    private fun clearIssueCouponDetailCache(couponId: Long) {
        Cache.delete(issueCouponDetailCacheKey(couponId))
    }

    private fun issueCouponDetailCacheKey(couponId: Long): String = "coupon:issue:detail:$couponId"

    companion object {
        private const val ISSUE_COUPON_DETAIL_CACHE_TTL_MINUTES = 60L
    }
}
