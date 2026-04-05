package com.coupon.storage.rdb.coupon

import com.coupon.enums.coupon.CouponIssueStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CouponIssueJpaRepository : JpaRepository<CouponIssueEntity, Long> {
    fun findAllByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<CouponIssueEntity>

    fun findAllByCouponId(
        couponId: Long,
        pageable: Pageable,
    ): Page<CouponIssueEntity>

    fun existsByUserIdAndCouponId(
        userId: Long,
        couponId: Long,
    ): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueEntity issue
           set issue.status = :usedStatus,
               issue.usedAt = :usedAt
         where issue.id = :couponIssueId
           and issue.status = :issuedStatus
        """,
    )
    fun useIfIssued(
        couponIssueId: Long,
        issuedStatus: CouponIssueStatus,
        usedStatus: CouponIssueStatus,
        usedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueEntity issue
           set issue.status = :canceledStatus,
               issue.canceledAt = :canceledAt
         where issue.id = :couponIssueId
           and issue.status = :issuedStatus
        """,
    )
    fun cancelIfIssued(
        couponIssueId: Long,
        issuedStatus: CouponIssueStatus,
        canceledStatus: CouponIssueStatus,
        canceledAt: LocalDateTime,
    ): Int
}
