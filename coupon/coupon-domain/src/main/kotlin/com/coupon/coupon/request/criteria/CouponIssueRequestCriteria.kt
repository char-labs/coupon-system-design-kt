package com.coupon.coupon.request.criteria

import com.coupon.coupon.request.command.CouponIssueRequestCommand
import com.coupon.enums.coupon.CouponIssueRequestStatus

sealed interface CouponIssueRequestCriteria {
    data class Create(
        val couponId: Long,
        val userId: Long,
        val idempotencyKey: String,
        val status: CouponIssueRequestStatus = CouponIssueRequestStatus.PENDING,
    ) : CouponIssueRequestCriteria {
        companion object {
            fun of(command: CouponIssueRequestCommand.Accept) =
                Create(
                    couponId = command.couponId,
                    userId = command.userId,
                    idempotencyKey = command.idempotencyKey,
                )
        }
    }
}
