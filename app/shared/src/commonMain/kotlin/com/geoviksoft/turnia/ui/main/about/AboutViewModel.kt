package com.geoviksoft.turnia.ui.main.about

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.interfaces.getPlatform
import com.geoviksoft.turnia.ui.system.AppVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AboutViewModel(
    userRepository: UserRepository,
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUi(
            userId = userRepository.loggedUser?.id?.value.orEmpty(),
            supportEmail = appConfigRepository.featureFlags.value.supportEmail,
        )
    )
    val uiState: StateFlow<AboutUi> = _uiState.asStateFlow()

    /** Support can only act on a report that says which build, which system and whose account. */
    fun feedbackRequested(kind: FeedbackKind, version: AppVersion) = _uiState.update {
        it.copy(
            feedbackMail = FeedbackMailUi(
                address = appConfigRepository.featureFlags.value.supportEmail,
                subject = kind.subject,
                prompt = kind.prompt,
                versionName = version.name,
                versionBuild = version.build,
                system = getPlatform().name,
                userId = it.userId,
            )
        )
    }

    fun feedbackMailOpened() = _uiState.update { it.copy(feedbackMail = null) }

    fun feedbackMailFailed() = _uiState.update {
        it.copy(feedbackMail = null, userMessage = AboutMessage.NoEmailApp)
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }
}
