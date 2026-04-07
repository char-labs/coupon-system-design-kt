package com.coupon.storage.rdb.coupon.request

import com.coupon.coupon.request.CouponIssueRequest
import com.coupon.coupon.request.criteria.CouponIssueRequestCriteria
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import com.coupon.storage.rdb.support.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "t_coupon_issue_request",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_coupon_issue_request_idempotency_key",
            columnNames = ["idempotency_key"],
        ),
    ],
    indexes = [
        Index(name = "idx_coupon_issue_request_status_created_at", columnList = "status, created_at"),
        Index(name = "idx_coupon_issue_request_coupon_id_created_at", columnList = "coupon_id, created_at"),
        Index(name = "idx_coupon_issue_request_user_id_created_at", columnList = "user_id, created_at"),
    ],
)
class CouponIssueRequestEntity(
    @Column(name = "coupon_id", nullable = false)
    var couponId: Long,
    @Column(name = "user_id", nullable = false)
    var userId: Long,
    @Column(name = "idempotency_key", nullable = false, length = 255)
    var idempotencyKey: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(20)")
    var status: CouponIssueRequestStatus = CouponIssueRequestStatus.PENDING,
    @Enumerated(EnumType.STRING)
    @Column(name = "result_code", columnDefinition = "varchar(30)")
    var resultCode: CouponCommandResultCode? = null,
    @Column(name = "coupon_issue_id")
    var couponIssueId: Long? = null,
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    var failureReason: String? = null,
    @Column(name = "enqueued_at")
    var enqueuedAt: LocalDateTime? = null,
    @Column(name = "processing_started_at")
    var processingStartedAt: LocalDateTime? = null,
    @Column(name = "delivery_attempt_count", nullable = false)
    var deliveryAttemptCount: Int = 0,
    @Column(name = "last_delivery_error", columnDefinition = "TEXT")
    var lastDeliveryError: String? = null,
    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,
) : BaseEntity() {
    constructor(criteria: CouponIssueRequestCriteria.Create) : this(
        couponId = criteria.couponId,
        userId = criteria.userId,
        idempotencyKey = criteria.idempotencyKey,
        status = criteria.status,
    )

    fun toCouponIssueRequest() =
        CouponIssueRequest(
            id = id!!,
            couponId = couponId,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = status,
            resultCode = resultCode,
            couponIssueId = couponIssueId,
            failureReason = failureReason,
            enqueuedAt = enqueuedAt,
            processingStartedAt = processingStartedAt,
            deliveryAttemptCount = deliveryAttemptCount,
            lastDeliveryError = lastDeliveryError,
            processedAt = processedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
