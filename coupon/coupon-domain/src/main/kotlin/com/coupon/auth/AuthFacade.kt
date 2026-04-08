package com.coupon.auth

import com.coupon.auth.command.AuthCommand
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.user.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthFacade(
    private val authService: AuthService,
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun signup(command: AuthCommand.SignUp): Token {
        userService.verifyEmail(command.email)
        val encodedPassword =
            passwordEncoder.encode(command.password)
                ?: error("PasswordEncoder.encode returned null")
        val user = userService.createUser(command.toUserCommand(encodedPassword))
        return authService.generateToken(AuthCommand.GenerateToken.toCommand(user))
    }

    @Transactional
    fun signin(command: AuthCommand.SignIn): Token {
        val credential = userService.getCredentialByEmail(command.email)

        if (!passwordEncoder.matches(command.password, credential.password)) {
            throw ErrorException(ErrorType.INVALID_PASSWORD)
        }

        return authService.generateToken(AuthCommand.GenerateToken.toCommand(credential))
    }
}
