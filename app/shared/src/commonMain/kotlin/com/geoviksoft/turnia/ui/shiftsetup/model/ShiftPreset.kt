package com.geoviksoft.turnia.ui.shiftsetup.model

import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.shift_preset_afternoon
import turnia.app.shared.generated.resources.shift_preset_afternoon_acronym
import turnia.app.shared.generated.resources.shift_preset_duty24
import turnia.app.shared.generated.resources.shift_preset_duty24_acronym
import turnia.app.shared.generated.resources.shift_preset_morning
import turnia.app.shared.generated.resources.shift_preset_morning_acronym
import turnia.app.shared.generated.resources.shift_preset_morning_afternoon
import turnia.app.shared.generated.resources.shift_preset_morning_afternoon_acronym
import turnia.app.shared.generated.resources.shift_preset_night
import turnia.app.shared.generated.resources.shift_preset_night_acronym

/**
 * The shifts the setup offers. They name no profession on purpose: the times are the ones most
 * rotas share, and the user edits them before anything is created.
 */
enum class ShiftPreset(
    val title: StringResource,
    val acronym: StringResource,
    val start: LocalTime,
    val end: LocalTime,
    val selectedByDefault: Boolean,
    val color: Color,
) {
    Morning(
        Res.string.shift_preset_morning, Res.string.shift_preset_morning_acronym,
        LocalTime(8, 0), LocalTime(15, 0), selectedByDefault = true, color = EntityPalette[3],
    ),
    Afternoon(
        Res.string.shift_preset_afternoon, Res.string.shift_preset_afternoon_acronym,
        LocalTime(15, 0), LocalTime(22, 0), selectedByDefault = true, color = EntityPalette[2],
    ),
    Night(
        Res.string.shift_preset_night, Res.string.shift_preset_night_acronym,
        LocalTime(22, 0), LocalTime(8, 0), selectedByDefault = true, color = EntityPalette[10],
    ),
    MorningAfternoon(
        Res.string.shift_preset_morning_afternoon, Res.string.shift_preset_morning_afternoon_acronym,
        LocalTime(8, 0), LocalTime(22, 0), selectedByDefault = false, color = EntityPalette[6],
    ),
    Duty24(
        Res.string.shift_preset_duty24, Res.string.shift_preset_duty24_acronym,
        LocalTime(8, 0), LocalTime(8, 0), selectedByDefault = false, color = EntityPalette[0],
    ),
}
