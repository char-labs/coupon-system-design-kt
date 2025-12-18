package com.coupon.auth

import com.coupon.auth.command.AuthCommand
import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.tx.Tx
import com.coupon.user.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthFacade(
    private val authService: AuthService,
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun signup(command: AuthCommand.SignUp): Token =
        Tx.writeable {
            userService.verifyEmail(command.email)
            val user = userService.createUser(command.toUserCommand(passwordEncoder.encode(command.password)!!))
            val token = authService.generateToken(AuthCommand.GenerateToken.toCommand(user))
            return@writeable token
        }

    fun signin(command: AuthCommand.SignIn): Token =
        Tx.writeable {
            val credential = userService.getCredentialByEmail(command.email)

            if (!passwordEncoder.matches(command.password, credential.password)) {
                throw ErrorException(ErrorType.INVALID_PASSWORD)
            }

            val token = authService.generateToken(AuthCommand.GenerateToken.toCommand(credential))
            return@writeable token
        }
}
