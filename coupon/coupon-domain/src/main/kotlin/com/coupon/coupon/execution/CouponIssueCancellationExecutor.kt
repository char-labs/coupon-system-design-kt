package com.coupon.coupon.execution

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.CouponService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.shared.lock.WithDistributedLock
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class CouponIssueCancellationExecutor(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
) {
    @WithDistributedLock(
        key = "'COUPON_ISSUE:' + #couponIssue.couponId",
        requiresNew = true,
    )
    fun cancelCoupon(
        couponIssue: CouponIssue.Detail,
        command: CouponIssueCommand.Cancel,
    ): CouponIssue.Detail {
        couponIssueService.cancelIssue(command)
        couponService.increaseQuantity(couponIssue.couponId)
        releaseStockSlotAfterCommit(couponIssue.couponId)
        return couponIssueService.getCouponIssue(command.couponIssueId)
    }

    private fun releaseStockSlotAfterCommit(couponId: Long) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            couponIssueService.releaseStockSlot(couponId)
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    couponIssueService.releaseStockSlot(couponId)
                }
            },
        )
    }
}
