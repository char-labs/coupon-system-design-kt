package com.coupon.redis.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StorageRedissonConfig(
    private val redisProperties: RedisProperties,
) {
    @Bean
    fun coreRedissonClient(): RedissonClient {
        val config = Config()
        config
            .useSingleServer()
            .setAddress("redis://${redisProperties.host}:${redisProperties.port}")
            .setConnectionMinimumIdleSize(10)
            .setConnectionPoolSize(50)
            .setConnectTimeout(3000)
            .retryAttempts = 3
        return Redisson.create(config)
    }
}
