package com.coupon.storage.rdb.coupon.activity

import com.coupon.coupon.activity.CouponActivity
import com.coupon.coupon.activity.criteria.CouponActivityCriteria
import com.coupon.enums.coupon.CouponActivityType
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
    name = "t_coupon_activity",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_coupon_activity_issue_type",
            columnNames = ["coupon_issue_id", "activity_type"],
        ),
    ],
    indexes = [
        Index(name = "idx_coupon_activity_coupon_id_occurred_at", columnList = "coupon_id, occurred_at"),
        Index(name = "idx_coupon_activity_user_id_occurred_at", columnList = "user_id, occurred_at"),
    ],
)
class CouponActivityEntity(
    @Column(name = "coupon_issue_id", nullable = false)
    var couponIssueId: Long,
    @Column(name = "coupon_id", nullable = false)
    var couponId: Long,
    @Column(name = "user_id", nullable = false)
    var userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, columnDefinition = "varchar(20)")
    var activityType: CouponActivityType,
    @Column(name = "occurred_at", nullable = false)
    var occurredAt: LocalDateTime,
) : BaseEntity() {
    constructor(criteria: CouponActivityCriteria.Create) : this(
        couponIssueId = criteria.couponIssueId,
        couponId = criteria.couponId,
        userId = criteria.userId,
        activityType = criteria.activityType,
        occurredAt = criteria.occurredAt,
    )

    fun toCouponActivity() =
        CouponActivity(
            id = id!!,
            couponIssueId = couponIssueId,
            couponId = couponId,
            userId = userId,
            activityType = activityType,
            occurredAt = occurredAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
