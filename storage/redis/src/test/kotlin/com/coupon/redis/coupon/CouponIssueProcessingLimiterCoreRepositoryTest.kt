package com.coupon.redis.coupon

import com.coupon.redis.config.CouponIssueProcessingLimitProperties
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.redisson.api.RRateLimiter
import org.redisson.api.RateType
import org.redisson.api.RedissonClient
import java.time.Duration

class CouponIssueProcessingLimiterCoreRepositoryTest :
    BehaviorSpec({
        given("CouponIssueProcessingLimiterCoreRepository로 permit을 획득하면") {
            `when`("processing limit이 enabled면") {
                val redissonClient = mockk<RedissonClient>()
                val rateLimiter = mockk<RRateLimiter>()
                val repository =
                    CouponIssueProcessingLimiterCoreRepository(
                        redissonClient = redissonClient,
                        properties = CouponIssueProcessingLimitProperties(enabled = true, permitsPerSecond = 100),
                    )

                every { redissonClient.getRateLimiter("coupon:issue:processing-rate-limiter") } returns rateLimiter
                every {
                    rateLimiter.trySetRate(
                        RateType.OVERALL,
                        100,
                        Duration.ofSeconds(1),
                    )
                } returns true
                justRun { rateLimiter.acquire(1) }

                repository.acquire()
                repository.acquire()

                then("cluster-wide rate limiter를 초기화하고 permit을 요청한다") {
                    verifySequence {
                        redissonClient.getRateLimiter("coupon:issue:processing-rate-limiter")
                        rateLimiter.trySetRate(
                            RateType.OVERALL,
                            100,
                            Duration.ofSeconds(1),
                        )
                        rateLimiter.acquire(1)
                        rateLimiter.acquire(1)
                    }
                }
            }

            `when`("processing limit이 disabled면") {
                val redissonClient = mockk<RedissonClient>()
                val repository =
                    CouponIssueProcessingLimiterCoreRepository(
                        redissonClient = redissonClient,
                        properties = CouponIssueProcessingLimitProperties(enabled = false, permitsPerSecond = 100),
                    )

                repository.acquire()

                then("Redis rate limiter를 호출하지 않는다") {
                    verify(exactly = 0) { redissonClient.getRateLimiter(any<String>()) }
                }
            }
        }
    })
