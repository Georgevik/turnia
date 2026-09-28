package com.geoviksoft.turnia.core.domain.model

/** How the user answered "Do you work with a team?". [value] is what analytics carries. */
enum class TeamPromptChoice(val value: String) {
    Create("create"),
    Join("join"),

    /** "Not now", or the sheet closed any other way. */
    Dismissed("dismissed"),
}
