package com.coupon.redis.coupon

import com.coupon.coupon.CouponIssueStateRepository
import com.coupon.enums.coupon.CouponIssueResult
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class CouponIssueRedisCoreRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) : CouponIssueStateRepository {
    override fun reserve(
        couponId: Long,
        userId: Long,
        totalQuantity: Long,
        ttl: Duration,
    ): CouponIssueResult {
        val result =
            redisTemplate.execute(
                reserveScript,
                listOf(occupiedCountKey(couponId), reservedUsersKey(couponId)),
                userId.toString(),
                totalQuantity.toString(),
                ttl.seconds.coerceAtLeast(1).toString(),
            ) ?: error("Coupon issue state script returned null")

        return when (result) {
            RESERVE_SUCCESS -> CouponIssueResult.SUCCESS
            RESERVE_DUPLICATE -> CouponIssueResult.DUPLICATE
            RESERVE_SOLD_OUT -> CouponIssueResult.SOLD_OUT
            else -> error("Unexpected coupon issue state result: $result")
        }
    }

    override fun release(
        couponId: Long,
        userId: Long,
    ) {
        redisTemplate.execute(
            releaseScript,
            listOf(occupiedCountKey(couponId), reservedUsersKey(couponId)),
            userId.toString(),
        )
    }

    override fun releaseStockSlot(couponId: Long) {
        redisTemplate.execute(
            releaseStockSlotScript,
            listOf(occupiedCountKey(couponId)),
        )
    }

    override fun rebuild(
        couponId: Long,
        occupiedCount: Long,
        userIds: Set<Long>,
        ttl: Duration,
    ) {
        val args =
            buildList {
                add(occupiedCount.coerceAtLeast(0).toString())
                add(ttl.seconds.coerceAtLeast(1).toString())
                userIds.sorted().mapTo(this) { it.toString() }
            }

        redisTemplate.execute(
            rebuildScript,
            listOf(occupiedCountKey(couponId), reservedUsersKey(couponId)),
            *args.toTypedArray(),
        )
    }

    override fun hasState(couponId: Long): Boolean = redisTemplate.hasKey(occupiedCountKey(couponId)) == true

    override fun clear(couponId: Long) {
        redisTemplate.delete(listOf(occupiedCountKey(couponId), reservedUsersKey(couponId)))
    }

    private fun occupiedCountKey(couponId: Long): String = "coupon:issue:state:$couponId:occupied-count"

    private fun reservedUsersKey(couponId: Long): String = "coupon:issue:state:$couponId:users"

    companion object {
        private const val RESERVE_SUCCESS = 0L
        private const val RESERVE_DUPLICATE = 1L
        private const val RESERVE_SOLD_OUT = 2L

        private val reserveScript =
            DefaultRedisScript<Long>().apply {
                resultType = Long::class.java
                setScriptText(
                    """
                    local occupiedKey = KEYS[1]
                    local usersKey = KEYS[2]
                    local userId = ARGV[1]
                    local totalQuantity = tonumber(ARGV[2])
                    local ttlSeconds = tonumber(ARGV[3])

                    if redis.call('SISMEMBER', usersKey, userId) == 1 then
                      return 1
                    end

                    local occupiedCount = tonumber(redis.call('GET', occupiedKey) or '0')
                    if occupiedCount >= totalQuantity then
                      return 2
                    end

                    occupiedCount = redis.call('INCR', occupiedKey)
                    redis.call('SADD', usersKey, userId)
                    redis.call('EXPIRE', occupiedKey, ttlSeconds)
                    redis.call('EXPIRE', usersKey, ttlSeconds)
                    return 0
                    """.trimIndent(),
                )
            }

        private val releaseScript =
            DefaultRedisScript<Long>().apply {
                resultType = Long::class.java
                setScriptText(
                    """
                    local occupiedKey = KEYS[1]
                    local usersKey = KEYS[2]
                    local userId = ARGV[1]

                    local removed = redis.call('SREM', usersKey, userId)
                    if removed == 1 then
                      local occupiedCount = tonumber(redis.call('GET', occupiedKey) or '0')
                      if occupiedCount > 0 then
                        redis.call('DECR', occupiedKey)
                      end
                    end

                    return removed
                    """.trimIndent(),
                )
            }

        private val releaseStockSlotScript =
            DefaultRedisScript<Long>().apply {
                resultType = Long::class.java
                setScriptText(
                    """
                    local occupiedKey = KEYS[1]
                    local occupiedCount = tonumber(redis.call('GET', occupiedKey) or '0')
                    if occupiedCount > 0 then
                      return redis.call('DECR', occupiedKey)
                    end

                    return occupiedCount
                    """.trimIndent(),
                )
            }

        private val rebuildScript =
            DefaultRedisScript<Long>().apply {
                resultType = Long::class.java
                setScriptText(
                    """
                    local occupiedKey = KEYS[1]
                    local usersKey = KEYS[2]
                    local occupiedCount = tonumber(ARGV[1])
                    local ttlSeconds = tonumber(ARGV[2])

                    redis.call('DEL', occupiedKey)
                    redis.call('DEL', usersKey)
                    redis.call('SET', occupiedKey, occupiedCount)
                    redis.call('EXPIRE', occupiedKey, ttlSeconds)

                    for i = 3, #ARGV do
                      redis.call('SADD', usersKey, ARGV[i])
                    end

                    redis.call('EXPIRE', usersKey, ttlSeconds)
                    return occupiedCount
                    """.trimIndent(),
                )
            }
    }
}
