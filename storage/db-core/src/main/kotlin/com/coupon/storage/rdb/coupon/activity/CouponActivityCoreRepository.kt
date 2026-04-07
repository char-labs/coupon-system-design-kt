package com.coupon.storage.rdb.coupon.activity

import com.coupon.coupon.activity.CouponActivity
import com.coupon.coupon.activity.CouponActivityRepository
import com.coupon.coupon.activity.criteria.CouponActivityCriteria
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class CouponActivityCoreRepository(
    private val couponActivityJpaRepository: CouponActivityJpaRepository,
) : CouponActivityRepository {
    override fun saveIfAbsent(criteria: CouponActivityCriteria.Create): CouponActivity =
        try {
            couponActivityJpaRepository
                .saveAndFlush(CouponActivityEntity(criteria))
                .toCouponActivity()
        } catch (exception: DataIntegrityViolationException) {
            if (!isDuplicateConstraintViolation(exception)) {
                throw exception
            }

            couponActivityJpaRepository
                .findByCouponIssueIdAndActivityType(
                    couponIssueId = criteria.couponIssueId,
                    activityType = criteria.activityType,
                )!!
                .toCouponActivity()
        }

    private fun isDuplicateConstraintViolation(exception: DataIntegrityViolationException): Boolean =
        generateSequence<Throwable>(exception) { it.cause }
            .mapNotNull { it.message }
            .any { message ->
                message.contains("uk_coupon_activity_issue_type", ignoreCase = true) ||
                    message.contains("t_coupon_activity", ignoreCase = true) &&
                    message.contains("coupon_issue_id", ignoreCase = true) &&
                    message.contains("activity_type", ignoreCase = true)
            }
}
