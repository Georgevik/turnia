package com.georgevik.turnia.ui.main.sharecalendar

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShareCalendarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShareCalendarUi())
    val uiState: StateFlow<ShareCalendarUi> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * TODO: read `users/{uid}.calendarSharedWith` and resolve each uid to a name and username.
     * Until then the screen renders its empty state, which is also what a user who shares with
     * nobody sees.
     */
    fun refresh() = Unit
}
