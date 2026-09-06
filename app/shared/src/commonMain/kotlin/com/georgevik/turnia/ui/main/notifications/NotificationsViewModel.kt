package com.georgevik.turnia.ui.main.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val userMessage = MutableStateFlow<NotificationsMessage?>(null)

    val uiState: StateFlow<NotificationsUi> =
        combine(userRepository.notificationsEnabled, userMessage) { enabled, message ->
            NotificationsUi(enabled = enabled, userMessage = message)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
            initialValue = NotificationsUi(),
        )

    fun onEnabledChanged(enabled: Boolean) {
        val uid = userRepository.loggedUser?.id ?: return

        viewModelScope.launch {
            userRepository.setNotificationsEnabled(uid, enabled)
                .onFailure { userMessage.value = NotificationsMessage.SaveFailed }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT = 5_000L
    }
}
