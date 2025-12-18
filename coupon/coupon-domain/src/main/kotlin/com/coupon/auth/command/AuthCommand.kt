package com.coupon.auth.command

import com.coupon.user.User
import com.coupon.user.UserCredential
import com.coupon.user.command.UserCommand

class AuthCommand {
    data class SignIn(
        val email: String,
        val password: String,
    )

    data class SignUp(
        val name: String,
        val email: String,
        val password: String,
    ) {
        fun toUserCommand(encryptedPassword: String) =
            UserCommand.Create(
                name = name,
                email = email,
                password = encryptedPassword,
            )
    }

    data class GenerateToken(
        val userId: Long,
        val userKey: String,
    ) {
        companion object {
            fun toCommand(user: User) =
                GenerateToken(
                    userId = user.id,
                    userKey = user.key,
                )

            fun toCommand(credential: UserCredential) =
                GenerateToken(
                    userId = credential.userId,
                    userKey = credential.userKey,
                )
        }
    }
}
