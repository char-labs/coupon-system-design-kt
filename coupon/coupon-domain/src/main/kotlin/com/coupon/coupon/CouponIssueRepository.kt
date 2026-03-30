package com.coupon.coupon

import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page

interface CouponIssueRepository {
    fun save(criteria: CouponIssueCriteria.Create): CouponIssue

    fun findById(couponIssueId: Long): CouponIssue

    fun findDetailById(couponIssueId: Long): CouponIssue.Detail

    fun findAllByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail>

    fun findAllByCouponId(
        couponId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail>

    fun existsByUserIdAndCouponId(
        userId: Long,
        couponId: Long,
    ): Boolean

    fun use(couponIssueId: Long)

    fun cancel(couponIssueId: Long)
}
