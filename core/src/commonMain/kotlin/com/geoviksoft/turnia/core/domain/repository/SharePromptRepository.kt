package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.SharePromptAnswer
import kotlinx.coroutines.flow.StateFlow

/**
 * When the app asks the user to share it: once per milestone of events added, all of it kept on
 * the device.
 */
interface SharePromptRepository {

    /** The prompt waiting to be shown, held as state until it has been: never a one-off event. */
    val pending: StateFlow<SharePrompt?>

    /**
     * Counts a new event — never an edit — and raises [pending] when it reaches a milestone.
     * Returns the device's count of events added, or 0 when it could not be written: the team
     * prompt counts on the same number rather than keeping a second one that would drift.
     */
    suspend fun eventAdded(kind: EventKind): Int

    /** The prompt is on screen: its milestone will not be offered again. */
    suspend fun shown(prompt: SharePrompt)

    /**
     * Clears [pending] and logs [answer], only if [prompt] is still the one waiting. Returns whether
     * it was, so a second tap before the sheet closes neither shares nor logs again.
     */
    fun answered(prompt: SharePrompt, answer: SharePromptAnswer): Boolean
}
