package com.coupon.outbox.notification.slack

interface SlackMessageSender {
    val enabled: Boolean

    fun sendMessage(message: SlackMessage)
}

data class SlackMessage(
    val text: String,
)
