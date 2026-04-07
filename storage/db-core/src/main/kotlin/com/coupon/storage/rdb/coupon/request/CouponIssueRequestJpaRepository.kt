package com.coupon.storage.rdb.coupon.request

import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CouponIssueRequestJpaRepository : JpaRepository<CouponIssueRequestEntity, Long> {
    fun findByIdempotencyKey(idempotencyKey: String): CouponIssueRequestEntity?

    fun findFirstByStatusOrderByUpdatedAtAscCreatedAtAsc(status: CouponIssueRequestStatus): CouponIssueRequestEntity?

    @Query(
        """
        select request
          from CouponIssueRequestEntity request
         where request.status in :statuses
           and coalesce(request.updatedAt, request.createdAt) < :updatedBefore
         order by coalesce(request.updatedAt, request.createdAt) asc,
                  request.id asc
        """,
    )
    fun findStaleByStatuses(
        statuses: Set<CouponIssueRequestStatus>,
        updatedBefore: LocalDateTime,
        pageable: Pageable,
    ): List<CouponIssueRequestEntity>

    @Query(
        """
        select request
          from CouponIssueRequestEntity request
         where request.status = :succeededStatus
           and (
               request.couponIssueId is null
               or not exists (
                   select issue.id
                     from CouponIssueEntity issue
                    where issue.id = request.couponIssueId
               )
           )
         order by coalesce(request.updatedAt, request.createdAt) asc,
                  request.id asc
        """,
    )
    fun findInconsistentSucceeded(
        succeededStatus: CouponIssueRequestStatus,
        pageable: Pageable,
    ): List<CouponIssueRequestEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :enqueuedStatus,
               request.enqueuedAt = :enqueuedAt,
               request.lastDeliveryError = null,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status in :candidateStatuses
        """,
    )
    fun markEnqueued(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        enqueuedStatus: CouponIssueRequestStatus,
        enqueuedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :enqueuedStatus,
               request.enqueuedAt = :enqueuedAt,
               request.processingStartedAt = null,
               request.lastDeliveryError = :lastDeliveryError,
               request.deliveryAttemptCount = request.deliveryAttemptCount + 1,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status in :candidateStatuses
        """,
    )
    fun markEnqueuedForRetry(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        enqueuedStatus: CouponIssueRequestStatus,
        enqueuedAt: LocalDateTime,
        lastDeliveryError: String,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :processingStatus,
               request.processingStartedAt = :processingStartedAt,
               request.failureReason = null,
               request.resultCode = null,
               request.lastDeliveryError = null,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status in :candidateStatuses
        """,
    )
    fun markProcessing(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        processingStatus: CouponIssueRequestStatus,
        processingStartedAt: LocalDateTime,
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
               request.lastDeliveryError = null,
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
               request.lastDeliveryError = null,
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
               request.lastDeliveryError = null,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.id = :requestId
           and request.status in :candidateStatuses
        """,
    )
    fun markDead(
        requestId: Long,
        candidateStatuses: Set<CouponIssueRequestStatus>,
        deadStatus: CouponIssueRequestStatus,
        resultCode: CouponCommandResultCode,
        failureReason: String,
        processedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CouponIssueRequestEntity request
           set request.status = :enqueuedStatus,
               request.resultCode = null,
               request.couponIssueId = null,
               request.failureReason = null,
               request.processingStartedAt = null,
               request.processedAt = null,
               request.lastDeliveryError = :lastDeliveryError,
               request.updatedAt = CURRENT_TIMESTAMP
         where request.status = :processingStatus
           and coalesce(request.updatedAt, request.createdAt) < :updatedBefore
        """,
    )
    fun recoverStuckProcessing(
        processingStatus: CouponIssueRequestStatus,
        enqueuedStatus: CouponIssueRequestStatus,
        updatedBefore: LocalDateTime,
        lastDeliveryError: String,
    ): Int
}
