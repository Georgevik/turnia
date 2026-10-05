package com.geoviksoft.turnia.ui.shiftsetup.model

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource

/** One shift on the list: a [preset], or one the user added, which carries its own [name]. */
data class ShiftRowUi(
    val id: String,
    val preset: ShiftPreset?,
    val name: String,
    val acronym: String,
    val start: String,
    val end: String,
    val color: Color,
    val selected: Boolean,
    /** Its time fields are open. Only looking: it changes nothing, so it is no interaction. */
    val expanded: Boolean = false,
) {
    val isCustom: Boolean get() = preset == null
}

data class CustomShiftForm(
    val name: String = "",
    val acronym: String = "",
    val start: String = "",
    val end: String = "",
    val nameMissing: Boolean = false,
    val acronymMissing: Boolean = false,
)

data class ShiftSetupUi(
    val rows: List<ShiftRowUi>,
    val custom: CustomShiftForm? = null,
    /** The user touched the panel: toggled, edited a time or started a shift of their own. */
    val interacted: Boolean = false,
    val saving: Boolean = false,
    val userMessage: StringResource? = null,
    /** The setup is over — created or skipped — and the screen has to leave. */
    val closed: Boolean = false,
) {
    val selectedCount: Int get() = rows.count { it.selected }
    val canConfirm: Boolean get() = selectedCount > 0 && !saving && !closed
}
