package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.core.system.Outcome
import kotlinx.coroutines.flow.StateFlow

/**
 * The screen that asks a new user which shifts they work, so their first event starts from a type
 * instead of a one-off. It decides when the screen is owed and reports how it went.
 */
interface ShiftSetupRepository {

    /**
     * The signed-in account has nothing to reuse — no personal type, no group, no invitation on the
     * way — and this device has not settled the setup yet. State, so a cold start cannot lose it.
     */
    val due: StateFlow<Boolean>

    /** The "tap a day" hint the calendar owes after the setup created the shifts, wherever it was opened. */
    val hintPending: StateFlow<Boolean>

    suspend fun shown(via: ShiftSetupVia)

    suspend fun skipped(via: ShiftSetupVia, interacted: Boolean)

    /** Creates [types] in one write; [customCount] of them were added by the user on the screen. */
    suspend fun complete(
        types: List<PersonalEventType>,
        via: ShiftSetupVia,
        interacted: Boolean,
        customCount: Int,
    ): Outcome<Unit, Unit>

    fun hintShown()
}
