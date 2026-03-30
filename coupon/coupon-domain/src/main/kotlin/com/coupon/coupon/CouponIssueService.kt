package com.coupon.coupon

import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.enums.CouponIssueStatus
import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.Lock
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service

@Service
class CouponIssueService(
    private val couponIssueRepository: CouponIssueRepository,
    private val couponRepository: CouponRepository,
    private val couponService: CouponService,
) {
    fun issueCoupon(command: CouponIssueCommand.Issue): CouponIssue =
        Lock.executeWithLockRequiresNew(
            key = "COUPON_ISSUE:${command.userId}:${command.couponId}",
        ) {
            if (couponIssueRepository.existsByUserIdAndCouponId(command.userId, command.couponId)) {
                throw ErrorException(ErrorType.ALREADY_ISSUED_COUPON)
            }
            couponService.validateCouponAvailability(command.couponId)

            val couponIssue = couponIssueRepository.save(CouponIssueCriteria.Create.of(command))
            couponRepository.decreaseQuantity(command.couponId)

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
        Tx.writeable {
            val couponIssue = couponIssueRepository.findById(command.couponIssueId)

            if (couponIssue.userId != command.userId) {
                throw ErrorException(ErrorType.FORBIDDEN_COUPON_ISSUE)
            }

            if (couponIssue.status != CouponIssueStatus.ISSUED) {
                throw ErrorException(ErrorType.INVALID_COUPON_STATUS)
            }

            couponIssueRepository.use(command.couponIssueId)

            couponIssueRepository.findDetailById(command.couponIssueId)
        }

    fun cancelCoupon(command: CouponIssueCommand.Cancel): CouponIssue.Detail =
        Tx.writeable {
            val couponIssue = couponIssueRepository.findById(command.couponIssueId)

            if (couponIssue.userId != command.userId) {
                throw ErrorException(ErrorType.FORBIDDEN_COUPON_ISSUE)
            }

            if (couponIssue.status != CouponIssueStatus.ISSUED) {
                throw ErrorException(ErrorType.INVALID_COUPON_STATUS)
            }

            couponIssueRepository.cancel(command.couponIssueId)

            couponRepository.increaseQuantity(couponIssue.couponId)

            couponIssueRepository.findDetailById(command.couponIssueId)
        }
}
