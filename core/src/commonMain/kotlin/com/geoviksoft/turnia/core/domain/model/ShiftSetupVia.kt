package com.geoviksoft.turnia.core.domain.model

import kotlinx.serialization.Serializable

/** Where the shift setup was opened from. [value] is what analytics carries. */
@Serializable
enum class ShiftSetupVia(val value: String) {
    /** On its own, after sign-in, to an account with nothing to reuse. */
    Onboarding("onboarding"),

    /** From the empty state of the day sheet's add pane. */
    AddPane("add_pane"),
}
