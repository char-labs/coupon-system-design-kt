package com.coupon.coupon

import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page

interface CouponIssueRepository {
    fun save(criteria: CouponIssueCriteria.Create): CouponIssue

    fun findById(couponIssueId: Long): CouponIssue

    fun findDetailById(couponIssueId: Long): CouponIssueDetail

    fun findAllByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssueDetail>

    fun findAllByCouponId(
        couponId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssueDetail>

    fun existsByUserIdAndCouponId(
        userId: Long,
        couponId: Long,
    ): Boolean

    fun use(couponIssueId: Long)

    fun cancel(couponIssueId: Long)
}
