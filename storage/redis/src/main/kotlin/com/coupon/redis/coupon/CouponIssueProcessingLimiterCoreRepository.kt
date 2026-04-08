package com.coupon.redis.coupon

import com.coupon.coupon.execution.CouponIssueProcessingLimiter
import com.coupon.redis.config.CouponIssueProcessingLimitProperties
import org.redisson.api.RateType
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class CouponIssueProcessingLimiterCoreRepository(
    @param:Qualifier("coreRedissonClient") private val redissonClient: RedissonClient,
    private val properties: CouponIssueProcessingLimitProperties,
) : CouponIssueProcessingLimiter {
    private val rateLimiter by lazy {
        redissonClient.getRateLimiter(RATE_LIMITER_KEY).apply {
            trySetRate(
                RateType.OVERALL,
                properties.permitsPerSecond,
                Duration.ofSeconds(1),
            )
        }
    }

    override fun acquire() {
        if (!properties.enabled) {
            return
        }

        rateLimiter.acquire(1)
    }

    companion object {
        private const val RATE_LIMITER_KEY = "coupon:issue:processing-rate-limiter"
    }
}
