package com.coupon.auth

import com.coupon.auth.command.AuthCommand
import com.coupon.auth.command.AuthenticationHistoryCommand
import com.coupon.auth.command.GenerateTokenCommand
import com.coupon.enums.auth.TokenStatus
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val tokenRepository: TokenRepository,
    private val authenticationHistoryRepository: AuthenticationHistoryRepository,
) {
    fun generateToken(command: AuthCommand.GenerateToken) =
        tokenRepository.create(command.userId, command.userKey).apply {
            authenticationHistoryRepository.create(
                AuthenticationHistoryCommand.Create(
                    userId = command.userId,
                    userKey = command.userKey,
                    command =
                        GenerateTokenCommand(
                            Token(
                                accessToken = this.accessToken,
                                refreshToken = this.refreshToken,
                            ),
                        ),
                    status = TokenStatus.ACTIVE,
                ),
            )
        }
}
