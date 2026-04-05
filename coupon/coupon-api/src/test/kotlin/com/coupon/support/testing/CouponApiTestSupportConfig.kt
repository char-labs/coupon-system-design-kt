package com.coupon.support.testing

import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class CouponApiTestSupportConfig {
    @Bean
    fun databaseCleaner(entityManager: EntityManager): DatabaseCleaner = DatabaseCleaner(entityManager)
}
