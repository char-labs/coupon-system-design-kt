package com.coupon.support.testing

import com.coupon.coupon.CouponIssueEventPublisher
import com.coupon.coupon.CouponIssueRedisRepository
import com.coupon.enums.coupon.CouponIssueResult
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@TestConfiguration
class CouponApiTestSupportConfig {
    @Bean
    fun databaseCleaner(entityManager: EntityManager): DatabaseCleaner = DatabaseCleaner(entityManager)

    @Bean
    @Primary
    fun couponIssueStateRepository(): CouponIssueRedisRepository = InMemoryCouponIssueRedisRepository()

    @Bean
    @Primary
    fun couponIssueEventPublisher(): CouponIssueEventPublisher = mockk(relaxed = true)
}

private class InMemoryCouponIssueRedisRepository : CouponIssueRedisRepository {
    private val states = ConcurrentHashMap<Long, IssueState>()

    override fun reserve(
        couponId: Long,
        userId: Long,
        totalQuantity: Long,
        ttl: Duration,
    ): CouponIssueResult {
        val state = states.computeIfAbsent(couponId) { IssueState() }

        return synchronized(state) {
            when {
                state.userIds.contains(userId) -> CouponIssueResult.DUPLICATE
                state.occupiedCount >= totalQuantity -> CouponIssueResult.SOLD_OUT
                else -> {
                    state.occupiedCount += 1
                    state.userIds.add(userId)
                    CouponIssueResult.SUCCESS
                }
            }
        }
    }

    override fun release(
        couponId: Long,
        userId: Long,
    ) {
        states[couponId]?.let { state ->
            synchronized(state) {
                if (state.userIds.remove(userId) && state.occupiedCount > 0) {
                    state.occupiedCount -= 1
                }
            }
        }
    }

    override fun releaseStockSlot(couponId: Long) {
        states[couponId]?.let { state ->
            synchronized(state) {
                if (state.occupiedCount > 0) {
                    state.occupiedCount -= 1
                }
            }
        }
    }

    override fun rebuild(
        couponId: Long,
        occupiedCount: Long,
        userIds: Set<Long>,
        ttl: Duration,
    ) {
        states[couponId] =
            IssueState(
                occupiedCount = occupiedCount.coerceAtLeast(0),
                userIds = userIds.toMutableSet(),
            )
    }

    override fun hasState(couponId: Long): Boolean = states.containsKey(couponId)

    override fun clear(couponId: Long) {
        states.remove(couponId)
    }

    private data class IssueState(
        var occupiedCount: Long = 0,
        val userIds: MutableSet<Long> = mutableSetOf(),
    )
}
