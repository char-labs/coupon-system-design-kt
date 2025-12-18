package com.coupon.storage.rdb.auth
import com.coupon.enums.AuthenticationEntityStatus
import org.springframework.data.jpa.repository.JpaRepository

interface AuthenticationHistoryJpaRepository : JpaRepository<AuthenticationHistoryEntity, Long> {
    fun findAllByUserKeyAndEntityStatus(
        userKey: String,
        status: AuthenticationEntityStatus,
    ): List<AuthenticationHistoryEntity>

    fun findByAccessToken(accessToken: String): AuthenticationHistoryEntity?

    fun findAllByUserKey(userKey: String): List<AuthenticationHistoryEntity>
}
