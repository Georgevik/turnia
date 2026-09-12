package com.geoviksoft.turnia.ui.main.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.ui.system.AppLanguage
import com.geoviksoft.turnia.ui.system.applyAppLanguage
import com.geoviksoft.turnia.ui.system.currentAppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PreferencesViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val userMessage = MutableStateFlow<PreferencesMessage?>(null)
    private val language = MutableStateFlow(currentAppLanguage())

    val uiState: StateFlow<PreferencesUi> =
        combine(userRepository.notificationsEnabled, language, userMessage) { enabled, language, message ->
            PreferencesUi(notificationsEnabled = enabled, language = language, userMessage = message)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
            initialValue = PreferencesUi(language = language.value),
        )

    fun onEnabledChanged(enabled: Boolean) {
        val uid = userRepository.loggedUser?.id ?: return

        viewModelScope.launch {
            userRepository.setNotificationsEnabled(uid, enabled)
                .onFailure { userMessage.value = PreferencesMessage.SaveFailed }
        }
    }

    fun onLanguageSelected(selected: AppLanguage) {
        if (selected == language.value) return
        applyAppLanguage(selected)
        language.value = selected
    }

    fun userMessageShown() {
        userMessage.value = null
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT = 5_000L
    }
}
