package com.coupon.coupon

import com.coupon.coupon.command.CouponCommand
import com.coupon.coupon.command.CouponPreviewCommand
import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.shared.cache.Cache
import com.coupon.shared.page.OffsetPageRequest
import com.coupon.shared.page.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference

@Service
@Transactional(readOnly = true)
class CouponService(
    private val couponRepository: CouponRepository,
    private val couponIssueRepository: CouponIssueRepository,
    private val couponCodeGenerator: CouponCodeGenerator,
    private val couponValidator: CouponValidator,
    private val couponEligibilityEvaluator: CouponEligibilityEvaluator,
    private val couponDiscountCalculator: CouponDiscountCalculator,
    private val cache: Cache,
) {
    /**
     * 쿠폰 마스터 데이터 생성은 관리자용 동기 write 경로다.
     * 요청이 끝나기 전에 coupon 테이블에 바로 반영된다.
     */
    @Transactional
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

    @Transactional
    fun decreaseQuantityIfAvailable(couponId: Long): Boolean = couponRepository.decreaseQuantityIfAvailable(couponId)

    @Transactional
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

    @Transactional
    fun modifyCoupon(
        couponId: Long,
        command: CouponCommand.Update,
    ): CouponDetail =
        couponRepository.update(couponId, CouponCriteria.Update.of(command)).also {
            cache.evict(issueCouponDetailCacheKey(couponId))
        }

    @Transactional
    fun activateCoupon(couponId: Long) {
        couponRepository.activate(couponId)
        cache.evict(issueCouponDetailCacheKey(couponId))
    }

    @Transactional
    fun deactivateCoupon(couponId: Long) {
        couponRepository.deactivate(couponId)
        cache.evict(issueCouponDetailCacheKey(couponId))
    }

    @Transactional
    fun deleteCoupon(couponId: Long) {
        couponRepository.delete(couponId)
        cache.evict(issueCouponDetailCacheKey(couponId))
    }

    private fun getCouponDetailForIssue(couponId: Long): CouponDetail =
        cache.getOrLoad(
            ttl = ISSUE_COUPON_DETAIL_CACHE_TTL_MINUTES,
            key = issueCouponDetailCacheKey(couponId),
            typeReference = object : TypeReference<CouponDetail>() {},
        ) {
            couponRepository.findDetailById(couponId)
        }

    private fun issueCouponDetailCacheKey(couponId: Long): String = "coupon:issue:detail:$couponId"

    companion object {
        private const val ISSUE_COUPON_DETAIL_CACHE_TTL_MINUTES = 60L
    }
}
