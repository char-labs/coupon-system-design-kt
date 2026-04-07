package com.coupon.coupon.request

import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.support.outbox.OutboxEventService
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CouponIssueRequestReconciliationService(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val outboxEventService: OutboxEventService,
) {
    /**
     * Self-healing pass for accepted requests.
     * It restores stale PROCESSING requests, re-creates missing relay outbox rows for stale PENDING requests,
     * and isolates impossible SUCCEEDED rows that do not have a persisted coupon issue.
     */
    fun reconcile(
        processingUpdatedBefore: LocalDateTime,
        pendingUpdatedBefore: LocalDateTime,
        batchSize: Int,
    ): CouponIssueRequestReconciliationSummary =
        Tx.writeable {
            val recoveredProcessingCount =
                couponIssueRequestRepository.recoverStuckProcessing(
                    updatedBefore = processingUpdatedBefore,
                )

            val stalePendingRequests =
                couponIssueRequestRepository.findStaleByStatuses(
                    statuses = setOf(CouponIssueRequestStatus.PENDING),
                    updatedBefore = pendingUpdatedBefore,
                    limit = batchSize,
                )

            val requeuedPendingCount =
                stalePendingRequests.count { request ->
                    val hasActiveOutboxEvent =
                        outboxEventService.existsActiveEvent(
                            aggregateType = CouponIssueRequestOutboxCommandFactory.AGGREGATE_TYPE,
                            aggregateId = request.id.toString(),
                        )

                    if (hasActiveOutboxEvent) {
                        false
                    } else {
                        outboxEventService.publish(CouponIssueRequestOutboxCommandFactory.issueRequested(request))
                        true
                    }
                }

            val inconsistentSucceededRequests = couponIssueRequestRepository.findInconsistentSucceeded(limit = batchSize)
            val isolatedInconsistentSucceededCount =
                inconsistentSucceededRequests.count { request ->
                    couponIssueRequestRepository.markDead(
                        requestId = request.id,
                        candidateStatuses = setOf(CouponIssueRequestStatus.SUCCEEDED),
                        resultCode = CouponCommandResultCode.UNKNOWN_ERROR,
                        failureReason = INCONSISTENT_SUCCEEDED_FAILURE_REASON,
                    )
                }

            CouponIssueRequestReconciliationSummary(
                recoveredProcessingCount = recoveredProcessingCount,
                scannedPendingCount = stalePendingRequests.size,
                requeuedPendingCount = requeuedPendingCount,
                scannedInconsistentSucceededCount = inconsistentSucceededRequests.size,
                isolatedInconsistentSucceededCount = isolatedInconsistentSucceededCount,
            )
        }

    companion object {
        const val INCONSISTENT_SUCCEEDED_FAILURE_REASON =
            "Reconciliation isolated inconsistent SUCCEEDED request without persisted coupon issue"
    }
}
