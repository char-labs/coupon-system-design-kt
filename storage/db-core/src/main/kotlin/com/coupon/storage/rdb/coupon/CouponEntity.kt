package com.coupon.storage.rdb.coupon

import com.coupon.coupon.Coupon
import com.coupon.coupon.CouponDetail
import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.enums.coupon.CouponStatus
import com.coupon.enums.coupon.CouponType
import com.coupon.storage.rdb.support.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "t_coupon",
)
class CouponEntity(
    @Column(name = "coupon_code", unique = true, nullable = false, length = 50)
    var couponCode: String,
    @Column(nullable = false, length = 100)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", columnDefinition = "varchar(20)", nullable = false)
    var couponType: CouponType,
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)", nullable = false)
    var status: CouponStatus = CouponStatus.ACTIVE,
    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Long,
    @Column(name = "max_discount_amount")
    var maxDiscountAmount: Long? = null,
    @Column(name = "min_order_amount")
    var minOrderAmount: Long? = null,
    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Long,
    @Column(name = "remaining_quantity", nullable = false)
    var remainingQuantity: Long,
    @Column(name = "available_at", nullable = false)
    var availableAt: LocalDateTime,
    @Column(name = "end_at", nullable = false)
    var endAt: LocalDateTime,
) : BaseEntity() {
    constructor(criteria: CouponCriteria.Create) : this(
        couponCode = criteria.couponCode,
        name = criteria.name,
        couponType = criteria.couponType,
        discountAmount = criteria.discountAmount,
        maxDiscountAmount = criteria.maxDiscountAmount,
        minOrderAmount = criteria.minOrderAmount,
        totalQuantity = criteria.totalQuantity,
        remainingQuantity = criteria.totalQuantity,
        availableAt = criteria.availableAt,
        endAt = criteria.endAt,
    )

    fun toCoupon() =
        Coupon(
            id = id!!,
            code = couponCode,
            name = name,
            type = couponType,
            status = status,
        )

    fun toCouponDetail() =
        CouponDetail(
            id = id!!,
            code = couponCode,
            name = name,
            type = couponType,
            status = status,
            discountAmount = discountAmount,
            maxDiscountAmount = maxDiscountAmount,
            minOrderAmount = minOrderAmount,
            totalQuantity = totalQuantity,
            remainingQuantity = remainingQuantity,
            availableAt = availableAt,
            endAt = endAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun update(criteria: CouponCriteria.Update) {
        criteria.name?.let { this.name = it }
        criteria.discountAmount?.let { this.discountAmount = it }
        criteria.maxDiscountAmount?.let { this.maxDiscountAmount = it }
        criteria.minOrderAmount?.let { this.minOrderAmount = it }
        criteria.availableAt?.let { this.availableAt = it }
        criteria.endAt?.let { this.endAt = it }
    }

    fun activate() {
        this.status = CouponStatus.ACTIVE
    }

    fun deactivate() {
        this.status = CouponStatus.INACTIVE
    }

    fun expire() {
        this.status = CouponStatus.EXPIRED
    }

    fun decreaseQuantity() {
        require(remainingQuantity > 0) { "남은 쿠폰 수량이 없습니다." }
        this.remainingQuantity -= 1
    }

    fun increaseQuantity() {
        this.remainingQuantity += 1
    }
}
