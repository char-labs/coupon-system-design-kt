package com.coupon.coupon

import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.Lock
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import org.springframework.stereotype.Service

@Service
class CouponIssueService(
    private val couponIssueRepository: CouponIssueRepository,
    private val couponRepository: CouponRepository,
    private val couponIssueValidator: CouponIssueValidator,
) {
    fun issueCoupon(command: CouponIssueCommand.Issue): CouponIssue =
        Lock.executeWithLockRequiresNew(
            key = "COUPON_ISSUE:${command.couponId}",
        ) {
            couponIssueValidator.validateIssuable(command.userId, command.couponId)

            val couponIssue = couponIssueRepository.save(CouponIssueCriteria.Create.of(command))
            val decreased = couponRepository.decreaseQuantityIfAvailable(command.couponId)
            if (!decreased) {
                throw ErrorException(ErrorType.COUPON_OUT_OF_STOCK)
            }

            couponIssue
        }

    fun getCouponIssue(couponIssueId: Long): CouponIssue.Detail = couponIssueRepository.findDetailById(couponIssueId)

    fun getMyCoupons(
        userId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail> = couponIssueRepository.findAllByUserId(userId, request)

    fun getCouponIssues(
        couponId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail> = couponIssueRepository.findAllByCouponId(couponId, request)

    fun useCoupon(command: CouponIssueCommand.Use): CouponIssue.Detail =
        Lock.executeWithLockRequiresNew(
            key = "COUPON_ISSUE_STATUS:${command.couponIssueId}",
        ) {
            val couponIssue = couponIssueRepository.findById(command.couponIssueId)
            couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId)

            val used = couponIssueRepository.useIfIssued(command.couponIssueId)
            if (!used) {
                throw ErrorException(ErrorType.INVALID_COUPON_STATUS)
            }

            couponIssueRepository.findDetailById(command.couponIssueId)
        }

    fun cancelCoupon(command: CouponIssueCommand.Cancel): CouponIssue.Detail {
        val couponIssue = couponIssueRepository.findById(command.couponIssueId)

        return Lock.executeWithLockRequiresNew(
            key = "COUPON_ISSUE:${couponIssue.couponId}",
        ) {
            couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId)
            val canceled = couponIssueRepository.cancelIfIssued(command.couponIssueId)
            if (!canceled) {
                throw ErrorException(ErrorType.INVALID_COUPON_STATUS)
            }

            couponRepository.increaseQuantity(couponIssue.couponId)

            couponIssueRepository.findDetailById(command.couponIssueId)
        }
    }
}
