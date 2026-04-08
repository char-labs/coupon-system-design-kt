package com.coupon.auth

import com.coupon.auth.command.AuthCommand
import com.coupon.auth.command.AuthenticationHistoryCommand
import com.coupon.auth.command.GenerateTokenCommand
import com.coupon.enums.auth.TokenStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val tokenRepository: TokenRepository,
    private val authenticationHistoryRepository: AuthenticationHistoryRepository,
) {
    @Transactional
    fun generateToken(command: AuthCommand.GenerateToken) =
        tokenRepository.save(command.userId, command.userKey).also { saved ->
            authenticationHistoryRepository.create(
                AuthenticationHistoryCommand.Create(
                    userId = command.userId,
                    userKey = command.userKey,
                    command =
                        GenerateTokenCommand(
                            Token(
                                accessToken = saved.accessToken,
                                refreshToken = saved.refreshToken,
                            ),
                        ),
                    status = TokenStatus.ACTIVE,
                ),
            )
        }
}
