package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import kotlinx.coroutines.flow.StateFlow

/**
 * When the app asks "Do you work with a team?": once per device, after enough events added, and
 * only to someone the server says belongs to no group.
 */
interface TeamPromptRepository {

    /** The prompt is waiting to be shown, held as state until it has been answered. */
    val pending: StateFlow<Boolean>

    /** [count] is the device's count of events added, as the share prompt keeps it. */
    suspend fun eventsAdded(count: Int)

    /** The prompt is on screen: it will not be offered on this device again. */
    suspend fun shown()

    /**
     * Clears [pending] and logs [choice], only if the prompt was still waiting. Returns whether it
     * was, so a second tap before the sheet closes neither navigates nor logs again.
     */
    fun answered(choice: TeamPromptChoice): Boolean
}
