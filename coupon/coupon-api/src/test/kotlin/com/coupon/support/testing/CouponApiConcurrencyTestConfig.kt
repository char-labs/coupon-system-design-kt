package com.coupon.support.testing

import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.shared.lock.LockRepository
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@TestConfiguration
class CouponApiConcurrencyTestConfig {
    @Bean
    @Primary
    fun lockRepository(): LockRepository = InMemoryLockRepository()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    allowedOriginPatterns = listOf("*")
                    allowedMethods = listOf("*")
                    allowedHeaders = listOf("*")
                    allowCredentials = true
                },
            )
        }
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
