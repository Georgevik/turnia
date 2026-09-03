package com.georgevik.turnia.ui.main.sharecalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShareCalendarViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareCalendarUi())
    val uiState: StateFlow<ShareCalendarUi> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            userRepository.getCalendarSharedWith().fold(
                onSuccess = { users ->
                    val rows = users.map { user ->
                        SharedUserUi(id = user.id, name = user.name, username = user.username)
                    }
                    _uiState.update { it.copy(loading = false, sharedWith = rows) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(loading = false, userMessage = ShareCalendarMessage.LoadFailed)
                    }
                },
            )
        }
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }
}
