package com.coupon.redis.lock

import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.LockRepository
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class LockCoreRepository(
    @param:Qualifier("coreRedissonClient") private val redissonClient: RedissonClient,
) : LockRepository {

    private val REDIS_LOCK_PREFIX = "lock:"

    override fun tryLock(key: String, timeoutMillis: Long): Boolean {
        val lock = redissonClient.getLock("$REDIS_LOCK_PREFIX$key")
        return lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS)
    }

    override fun unlock(key: String) {
        val lock = redissonClient.getLock("$REDIS_LOCK_PREFIX$key")
        if (lock.isHeldByCurrentThread) lock.unlock()
    }

    override fun <T> executeWithLock(
        key: String,
        timeoutMillis: Long,
        timeoutException: ErrorType,
        func: () -> T,
    ): T {
        val lockSuccess = tryLock(key, timeoutMillis)
        if (!lockSuccess) throw ErrorException(timeoutException)
        try {
            return func()
        } finally {
            unlock(key)
        }
    }
}
