package com.georgevik.turnia.ui.main.sharecalendar

data class ShareCalendarUi(
    val loading: Boolean = false,
    val sharedWith: List<SharedUserUi> = emptyList(),
)

/** Someone this user granted read access to their calendar. */
data class SharedUserUi(
    val id: String,
    val name: String,
    val username: String,
)
