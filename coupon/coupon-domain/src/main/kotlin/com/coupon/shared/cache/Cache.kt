package com.coupon.shared.cache

import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

@Component
class Cache(
    private val cacheExecutor: CacheExecutor,
) {
    fun <T> getOrLoad(
        ttl: Long,
        key: String,
        typeReference: TypeReference<T>,
        function: () -> T,
    ): T = cacheExecutor.invoke(ttl, key, typeReference, function)

    fun <T> putValue(
        key: String,
        value: T,
        ttl: Long,
    ) {
        cacheExecutor.put(key, value, ttl)
    }

    fun evict(key: String) {
        cacheExecutor.delete(key)
    }
}

@Component
class CacheExecutor(
    private val cacheRepository: CacheRepository,
    private val objectMapper: ObjectMapper,
) {
    fun <T> invoke(
        ttl: Long,
        key: String,
        typeReference: TypeReference<T>,
        function: () -> T,
    ): T {
        val cached = cacheRepository.get(key)
        if (cached != null) {
            return objectMapper.readValue(cached, typeReference)
        }

        val result = function()

        val serialized = objectMapper.writeValueAsString(result)
        cacheRepository.put(key, serialized, ttl)
        return result
    }

    fun <T> put(
        key: String,
        value: T,
        ttl: Long,
    ) = cacheRepository.put(key, objectMapper.writeValueAsString(value), ttl)

    fun delete(key: String) = cacheRepository.delete(key)
}
