package com.coupon.support.testing

import com.coupon.auth.AuthenticationHistoryRepository
import com.coupon.auth.TokenRepository
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.LockRepository
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
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
