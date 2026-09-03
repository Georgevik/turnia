package com.georgevik.turnia.ui.main.sharecalendar

data class ShareCalendarUi(
    val loading: Boolean = true,
    val sharedWith: List<SharedUserUi> = emptyList(),
    val userMessage: ShareCalendarMessage? = null,
)

data class SharedUserUi(
    val id: String,
    val name: String,
    val username: String,
)

enum class ShareCalendarMessage { LoadFailed }
