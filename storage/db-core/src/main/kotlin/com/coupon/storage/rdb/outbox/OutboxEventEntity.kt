package com.coupon.storage.rdb.outbox

import com.coupon.shared.outbox.OutboxEvent
import com.coupon.shared.outbox.OutboxEventStatus
import com.coupon.shared.outbox.criteria.OutboxEventCriteria
import com.coupon.storage.rdb.support.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "t_outbox_event",
    indexes = [
        Index(name = "idx_outbox_event_status_available_id", columnList = "status, available_at, id"),
        Index(name = "idx_outbox_event_dedupe_key", columnList = "dedupe_key"),
        Index(name = "idx_outbox_event_aggregate", columnList = "aggregate_type, aggregate_id"),
    ],
)
class OutboxEventEntity(
    @Column(name = "event_type", nullable = false, length = 100)
    var eventType: String,
    @Column(name = "aggregate_type", nullable = false, length = 100)
    var aggregateType: String,
    @Column(name = "aggregate_id", nullable = false, length = 100)
    var aggregateId: String,
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    var payloadJson: String,
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)", nullable = false)
    var status: OutboxEventStatus = OutboxEventStatus.PENDING,
    @Column(name = "dedupe_key", length = 255)
    var dedupeKey: String? = null,
    @Column(name = "available_at", nullable = false)
    var availableAt: LocalDateTime,
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,
    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,
) : BaseEntity() {
    constructor(criteria: OutboxEventCriteria.Create) : this(
        eventType = criteria.eventType,
        aggregateType = criteria.aggregateType,
        aggregateId = criteria.aggregateId,
        payloadJson = criteria.payloadJson,
        status = criteria.status,
        dedupeKey = criteria.dedupeKey,
        availableAt = criteria.availableAt,
        retryCount = criteria.retryCount,
        lastError = criteria.lastError,
        processedAt = criteria.processedAt,
    )

    fun toOutboxEvent() =
        OutboxEvent(
            id = id!!,
            eventType = eventType,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            payloadJson = payloadJson,
            status = status,
            dedupeKey = dedupeKey,
            availableAt = availableAt,
            retryCount = retryCount,
            lastError = lastError,
            processedAt = processedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
