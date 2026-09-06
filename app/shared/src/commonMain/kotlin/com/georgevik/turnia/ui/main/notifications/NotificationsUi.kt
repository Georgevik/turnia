package com.georgevik.turnia.ui.main.notifications

data class NotificationsUi(
    val enabled: Boolean = true,
    val userMessage: NotificationsMessage? = null,
)

enum class NotificationsMessage { SaveFailed }
