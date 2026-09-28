package com.geoviksoft.turnia.ui.components.teamprompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.TeamPromptRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TeamPromptViewModel(
    private val teamPromptRepository: TeamPromptRepository,
    private val invitationLinkRepository: InvitationLinkRepository,
) : ViewModel() {

    val pending: StateFlow<Boolean> = teamPromptRepository.pending

    fun shown() {
        viewModelScope.launch { teamPromptRepository.shown() }
    }

    /** Returns whether the answer counted, so a second tap before the sheet closes does nothing. */
    fun answered(choice: TeamPromptChoice): Boolean {
        if (!teamPromptRepository.answered(choice)) return false
        // Main brings the Groups tab up and the tab opens the sheet: the prompt only asks for it.
        if (choice == TeamPromptChoice.Join) invitationLinkRepository.requestJoinSheet()
        return true
    }
}
