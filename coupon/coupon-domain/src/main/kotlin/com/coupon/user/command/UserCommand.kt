package com.coupon.user.command

sealed class UserCommand {
    data class Create(
        val name: String,
        val email: String,
        val password: String,
    ) : UserCommand()

    data class Update(
        val name: String?,
        val email: String?,
        val password: String?,
    ) : UserCommand()
}
