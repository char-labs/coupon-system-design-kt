package com.coupon.storage.rdb.auth

import com.coupon.auth.AuthenticationHistory
import com.coupon.auth.Token
import com.coupon.auth.command.AuthenticationHistoryCommand
import com.coupon.enums.AuthenticationEntityStatus
import com.coupon.enums.TokenStatus
import com.coupon.storage.rdb.support.AuthenticationBaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "t_authentication_history",
    indexes = [
        Index(name = "idx_authentication_history_user_id", columnList = "user_id"),
        Index(name = "idx_authentication_history_user_key", columnList = "user_key"),
        Index(name = "idx_authentication_history_access_token", columnList = "access_token"),
    ],
)
class AuthenticationHistoryEntity(
    val userId: Long,
    val userKey: String,
    @Column(columnDefinition = "TEXT")
    var accessToken: String,
    @Column(columnDefinition = "TEXT")
    var refreshToken: String,
) : AuthenticationBaseEntity() {
    constructor(
        create: AuthenticationHistoryCommand.Create,
    ) : this(
        userId = create.userId,
        userKey = create.userKey,
        accessToken = create.command.token.accessToken,
        refreshToken = create.command.token.refreshToken,
    )

    fun toAuthenticationHistory(): AuthenticationHistory =
        AuthenticationHistory(
            authenticationId = id!!,
            userKey = userKey,
            token =
                Token(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                ),
            status = entityStatus.toTokenStatus(),
            loggedInAt = updatedAt ?: createdAt,
        )

    fun updateRefreshToken(token: Token): AuthenticationHistory {
        this.accessToken = token.accessToken
        this.refreshToken = token.refreshToken
        return AuthenticationHistory(
            authenticationId = id!!,
            userKey = userKey,
            token =
                Token(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                ),
            status = entityStatus.toTokenStatus(),
            loggedInAt = updatedAt ?: createdAt,
        )
    }

    internal fun AuthenticationEntityStatus.toTokenStatus(): TokenStatus =
        when (this) {
            AuthenticationEntityStatus.ACTIVE -> TokenStatus.ACTIVE
            AuthenticationEntityStatus.DELETE -> TokenStatus.INACTIVE
        }
}
