package com.geoviksoft.turnia.ui.components.shareprompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.SharePromptAnswer
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SharePromptViewModel(
    private val sharePromptRepository: SharePromptRepository,
) : ViewModel() {

    val pending: StateFlow<SharePrompt?> = sharePromptRepository.pending

    fun shown(prompt: SharePrompt) {
        viewModelScope.launch { sharePromptRepository.shown(prompt) }
    }

    fun answered(prompt: SharePrompt, answer: SharePromptAnswer): Boolean =
        sharePromptRepository.answered(prompt, answer)
}
