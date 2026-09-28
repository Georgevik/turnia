package com.geoviksoft.turnia.ui.shiftsetup

import com.geoviksoft.turnia.ui.shiftsetup.model.CustomShiftForm
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftPreset
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftRowUi
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.createUuid

/** Builds the setup's rows: one per preset, and one for each shift the user adds on the screen. */
class ShiftRowFactory {

    fun presets(): List<ShiftRowUi> = ShiftPreset.entries.map { preset ->
        ShiftRowUi(
            id = preset.name,
            preset = preset,
            name = "",
            acronym = "",
            start = preset.start.toString(),
            end = preset.end.toString(),
            color = preset.color,
            selected = preset.selectedByDefault,
        )
    }

    /** A shift of the user's own takes the first colour no other shift on the list wears. */
    fun custom(form: CustomShiftForm, rows: List<ShiftRowUi>): ShiftRowUi {
        val used = rows.map { it.color }.toSet()
        return ShiftRowUi(
            id = "custom-${createUuid()}",
            preset = null,
            name = form.name.trim(),
            acronym = form.acronym.trim(),
            start = form.start,
            end = form.end,
            color = EntityPalette.firstOrNull { it !in used } ?: EntityPalette.first(),
            selected = true,
        )
    }
}
