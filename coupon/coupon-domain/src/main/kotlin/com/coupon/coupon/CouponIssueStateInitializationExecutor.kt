package com.coupon.coupon

import com.coupon.shared.lock.WithDistributedLock
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CouponIssueStateInitializationExecutor(
    private val couponIssueRepository: CouponIssueRepository,
    private val couponIssueRedisRepository: CouponIssueRedisRepository,
) {
    @WithDistributedLock(
        key = "'COUPON_ISSUE_STATE_INIT:' + #coupon.id",
        timeoutMillis = 1_000L,
    )
    fun initializeStateIfAbsent(
        coupon: CouponDetail,
        ttl: Duration,
    ) {
        if (couponIssueRedisRepository.hasState(coupon.id)) {
            return
        }

        val issuedUsers = couponIssueRepository.findUserIdsByCouponId(coupon.id)
        val occupiedCount = coupon.totalQuantity - coupon.remainingQuantity
        couponIssueRedisRepository.rebuild(
            couponId = coupon.id,
            occupiedCount = occupiedCount,
            userIds = issuedUsers,
            ttl = ttl,
        )
    }

    @WithDistributedLock(
        key = "'COUPON_ISSUE_STATE_INIT:' + #coupon.id",
        timeoutMillis = 1_000L,
    )
    fun rebuildState(
        coupon: CouponDetail,
        ttl: Duration,
    ) {
        val issuedUsers = couponIssueRepository.findUserIdsByCouponId(coupon.id)
        val occupiedCount = coupon.totalQuantity - coupon.remainingQuantity
        couponIssueRedisRepository.rebuild(
            couponId = coupon.id,
            occupiedCount = occupiedCount,
            userIds = issuedUsers,
            ttl = ttl,
        )
    }
}
