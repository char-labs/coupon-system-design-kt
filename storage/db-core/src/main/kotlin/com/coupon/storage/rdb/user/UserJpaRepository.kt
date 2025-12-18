package com.coupon.storage.rdb.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByUserKey(key: String): UserEntity?

    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): UserEntity?

    fun findAllBy(pageable: Pageable): Page<UserEntity>
}
