package com.coupon.support.testing

import com.coupon.auth.AuthenticationHistoryRepository
import com.coupon.auth.TokenRepository
import com.coupon.coupon.CouponIssueStateRepository
import com.coupon.coupon.execution.CouponIssueProcessingLimiter
import com.coupon.coupon.intake.CouponIssueMessagePublisher
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.outbox.notification.slack.SlackMessageSender
import com.coupon.shared.lock.LockRepository
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@TestConfiguration
class CouponWorkerTestSupportConfig {
    @Bean
    fun databaseCleaner(entityManager: EntityManager): DatabaseCleaner = DatabaseCleaner(entityManager)

    @Bean
    @Primary
    fun lockRepository(): LockRepository = InMemoryLockRepository()

    @Bean
    fun tokenRepository(): TokenRepository = mockk(relaxed = true)

    @Bean
    fun authenticationHistoryRepository(): AuthenticationHistoryRepository = mockk(relaxed = true)

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun slackMessageSender(): SlackMessageSender =
        object : SlackMessageSender {
            override val enabled: Boolean = false

            override fun sendMessage(message: com.coupon.outbox.notification.slack.SlackMessage) = Unit
        }

    @Bean
    @Primary
    fun couponIssueStateRepository(): CouponIssueStateRepository = InMemoryCouponIssueStateRepository()

    @Bean
    @Primary
    fun couponIssueMessagePublisher(): CouponIssueMessagePublisher = mockk(relaxed = true)

    @Bean
    @Primary
    fun couponIssueProcessingLimiter(): CouponIssueProcessingLimiter = mockk(relaxed = true)
}

private class InMemoryLockRepository : LockRepository {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    override fun tryLock(
        key: String,
        timeoutMillis: Long,
    ): Boolean = locks.computeIfAbsent(key) { ReentrantLock() }.tryLock(timeoutMillis, TimeUnit.MILLISECONDS)

    override fun unlock(key: String) {
        val lock = locks[key] ?: return
        if (lock.isHeldByCurrentThread) {
            lock.unlock()
        }
    }

    override fun <T> executeWithLock(
        key: String,
        timeoutMillis: Long,
        timeoutException: ErrorType,
        func: () -> T,
    ): T {
        val lockSuccess = tryLock(key, timeoutMillis)
        if (!lockSuccess) {
            throw ErrorException(timeoutException)
        }

        return try {
            func()
        } finally {
            unlock(key)
        }
    }
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
