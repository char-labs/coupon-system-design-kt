package com.coupon.storage.rdb.coupon.request

import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CouponIssueRequestJpaRepository : JpaRepository<CouponIssueRequestEntity, Long> {
    fun findByIdempotencyKey(idempotencyKey: String): CouponIssueRequestEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :processingStatus,
               request.failureReason = null,
               request.resultCode = null,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status in :candidateStatuses
        """,
    )
    fun markProcessing(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        processingStatus: CouponIssueRequestStatus,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :succeededStatus,
               request.couponIssueId = :couponIssueId,
               request.processedAt = :processedAt,
               request.failureReason = null,
               request.resultCode = null,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status = :processingStatus
        """,
    )
    fun markSucceeded(
        requestId: Long,
        processingStatus: CouponIssueRequestStatus,
        succeededStatus: CouponIssueRequestStatus,
        couponIssueId: Long,
        processedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :failedStatus,
               request.resultCode = :resultCode,
               request.failureReason = :failureReason,
               request.processedAt = :processedAt,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status = :processingStatus
        """,
    )
    fun markFailed(
        requestId: Long,
        processingStatus: CouponIssueRequestStatus,
        failedStatus: CouponIssueRequestStatus,
        resultCode: CouponCommandResultCode,
        failureReason: String,
        processedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :deadStatus,
               request.resultCode = :resultCode,
               request.failureReason = :failureReason,
               request.processedAt = :processedAt,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status = :processingStatus
        """,
    )
    fun markDead(
        requestId: Long,
        processingStatus: CouponIssueRequestStatus,
        deadStatus: CouponIssueRequestStatus,
        resultCode: CouponCommandResultCode,
        failureReason: String,
        processedAt: LocalDateTime,
    ): Int
}
