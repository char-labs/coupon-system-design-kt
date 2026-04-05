package com.coupon.storage.rdb.auth

import com.coupon.auth.AuthenticationHistory
import com.coupon.auth.AuthenticationHistoryRepository
import com.coupon.auth.command.AuthenticationHistoryCommand
import com.coupon.enums.auth.AuthenticationEntityStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class AuthenticationHistoryCoreRepository(
    private val repository: AuthenticationHistoryJpaRepository,
) : AuthenticationHistoryRepository {
    @Transactional
    override fun create(authenticationHistoryCommand: AuthenticationHistoryCommand.Create): AuthenticationHistory {
        val saveHistory = repository.save(AuthenticationHistoryEntity(authenticationHistoryCommand))
        return saveHistory.toAuthenticationHistory()
    }

    override fun findByUserKeyWithRefreshToken(
        userKey: String,
        refreshToken: String,
    ): AuthenticationHistory {
        val histories =
            repository.findAllByUserKey(
                userKey = userKey,
            )
        return histories.find { it.refreshToken == refreshToken }?.toAuthenticationHistory()
            ?: throw ErrorException(ErrorType.NOT_FOUND_DATA)
    }

    @Transactional
    override fun update(command: AuthenticationHistoryCommand.Update): AuthenticationHistory? {
        val histories =
            repository.findAllByUserKey(
                userKey = command.userKey,
            )
        return histories
            .find {
                it.refreshToken == command.refreshToken
            }?.updateRefreshToken(command.command.token)
    }

    @Transactional
    override fun removeToken(userKey: String): List<String> =
        repository.findAllByUserKeyAndEntityStatus(userKey, AuthenticationEntityStatus.ACTIVE).map {
            it.delete()
            it.accessToken
        }

    @Transactional
    override fun remove(token: String): String {
        val authenticationHistory =
            repository.findByAccessToken(token)
                ?: throw ErrorException(ErrorType.NOT_FOUND_DATA)
        authenticationHistory.delete()
        return authenticationHistory.refreshToken
    }
}
