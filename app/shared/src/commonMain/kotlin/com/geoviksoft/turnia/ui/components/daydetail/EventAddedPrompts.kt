package com.geoviksoft.turnia.ui.components.daydetail

import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import com.geoviksoft.turnia.core.domain.repository.TeamPromptRepository

/**
 * Tells both calendar prompts that an event was added. The share prompt keeps the device's count;
 * the team prompt is handed that same number rather than keeping one of its own.
 */
class EventAddedPrompts(
    private val sharePromptRepository: SharePromptRepository,
    private val teamPromptRepository: TeamPromptRepository,
) {

    suspend fun added(kind: EventKind) {
        val count = sharePromptRepository.eventAdded(kind)
        if (count > 0) teamPromptRepository.eventsAdded(count)
    }
}
