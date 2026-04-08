package com.coupon.support.testing

import com.coupon.coupon.CouponIssueStateRepository
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.execution.CouponIssueExecutionFacade
import com.coupon.coupon.intake.CouponIssueMessage
import com.coupon.coupon.intake.CouponIssueMessagePublisher
import com.coupon.coupon.intake.CouponIssuePublishReceipt
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.shared.cache.CacheRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.ObjectProvider
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
    fun couponIssueStateRepository(): CouponIssueStateRepository = InMemoryCouponIssueStateRepository()

    @Bean
    @Primary
    fun cacheRepository(): CacheRepository = InMemoryCacheRepository()

    @Bean
    @Primary
    fun couponIssueMessagePublisher(
        couponIssueExecutionFacadeProvider: ObjectProvider<CouponIssueExecutionFacade>,
    ): CouponIssueMessagePublisher = SynchronousCouponIssueMessagePublisher(couponIssueExecutionFacadeProvider)
}

private class InMemoryCouponIssueStateRepository : CouponIssueStateRepository {
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

private class InMemoryCacheRepository : CacheRepository {
    private val values = ConcurrentHashMap<String, String>()

    override fun get(key: String): String? = values[key]

    override fun put(
        key: String,
        value: String,
        ttl: Long,
    ) {
        values[key] = value
    }

    override fun delete(key: String) {
        values.remove(key)
    }
}

private class SynchronousCouponIssueMessagePublisher(
    private val couponIssueExecutionFacadeProvider: ObjectProvider<CouponIssueExecutionFacade>,
) : CouponIssueMessagePublisher {
    override fun publish(message: CouponIssueMessage): CouponIssuePublishReceipt {
        couponIssueExecutionFacadeProvider.getObject().executeIssue(
            CouponIssueCommand.Issue(
                couponId = message.couponId,
                userId = message.userId,
            ),
        )

        return CouponIssuePublishReceipt(
            topic = "sync.coupon.issue",
            partition = 0,
            offset = 0,
        )
    }
}
