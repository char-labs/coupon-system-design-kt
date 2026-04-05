package com.coupon.storage.rdb.coupon

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.enums.CouponIssueStatus
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
    name = "t_coupon_issue",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_coupon_issue_user_coupon",
            columnNames = ["user_id", "coupon_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_coupon_issue_user_id", columnList = "user_id"),
        Index(name = "idx_coupon_issue_coupon_id", columnList = "coupon_id"),
    ],
)
class CouponIssueEntity(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)", nullable = false)
    var status: CouponIssueStatus = CouponIssueStatus.ISSUED,
    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,
    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null,
) : BaseEntity() {
    constructor(criteria: CouponIssueCriteria.Create) : this(
        userId = criteria.userId,
        couponId = criteria.couponId,
    )

    fun toCouponIssue() =
        CouponIssue(
            id = id!!,
            couponId = couponId,
            userId = userId,
            status = status,
        )

    fun toCouponIssueDetail(
        couponCode: String,
        couponName: String,
    ) = CouponIssue.Detail(
        id = id!!,
        couponId = couponId,
        couponCode = couponCode,
        couponName = couponName,
        userId = userId,
        status = status,
        issuedAt = createdAt,
        usedAt = usedAt,
        canceledAt = canceledAt,
    )
}
