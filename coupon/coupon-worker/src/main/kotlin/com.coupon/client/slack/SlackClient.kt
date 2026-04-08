package com.coupon.client.slack

interface SlackClient {
    val enabled: Boolean

    fun sendMessage(message: SlackMessage)
}

data class SlackMessage(
    val text: String,
)
