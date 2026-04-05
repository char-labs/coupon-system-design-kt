package com.coupon.coupon.fixture

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.enums.coupon.CouponIssueStatus
import java.time.LocalDateTime

internal object CouponIssueFixtures {
    fun issued(
        id: Long = 1L,
        couponId: Long = 10L,
        userId: Long = 100L,
        status: CouponIssueStatus = CouponIssueStatus.ISSUED,
    ) = CouponIssue(
        id = id,
        couponId = couponId,
        userId = userId,
        status = status,
    )

    fun detail(
        id: Long = 1L,
        couponId: Long = 10L,
        couponCode: String = "CP-$couponId",
        couponName: String = "쿠폰-$couponId",
        userId: Long = 100L,
        status: CouponIssueStatus = CouponIssueStatus.ISSUED,
        referenceTime: LocalDateTime = LocalDateTime.now(),
        issuedAt: LocalDateTime = referenceTime.minusDays(1),
        usedAt: LocalDateTime? = null,
        canceledAt: LocalDateTime? = null,
    ) = CouponIssue.Detail(
        id = id,
        couponId = couponId,
        couponCode = couponCode,
        couponName = couponName,
        userId = userId,
        status = status,
        issuedAt = issuedAt,
        usedAt = usedAt,
        canceledAt = canceledAt,
    )

    fun issueCommand(
        couponId: Long = 10L,
        userId: Long = 100L,
    ) = CouponIssueCommand.Issue(
        couponId = couponId,
        userId = userId,
    )

    fun useCommand(
        couponIssueId: Long = 1L,
        userId: Long = 100L,
    ) = CouponIssueCommand.Use(
        couponIssueId = couponIssueId,
        userId = userId,
    )

    fun cancelCommand(
        couponIssueId: Long = 1L,
        userId: Long = 100L,
    ) = CouponIssueCommand.Cancel(
        couponIssueId = couponIssueId,
        userId = userId,
    )
}
