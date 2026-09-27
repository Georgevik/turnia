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

    /** Counts a new event — never an edit — and raises [pending] when it reaches a milestone. */
    suspend fun eventAdded(kind: EventKind)

    /** The prompt is on screen: its milestone will not be offered again. */
    suspend fun shown(prompt: SharePrompt)

    fun answered(prompt: SharePrompt, answer: SharePromptAnswer)
}
