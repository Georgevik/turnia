package com.geoviksoft.turnia.ui.shiftsetup

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
    val color: String,
) {
    Morning(
        Res.string.shift_preset_morning, Res.string.shift_preset_morning_acronym,
        LocalTime(8, 0), LocalTime(15, 0), selectedByDefault = true, color = "#F9A825",
    ),
    Afternoon(
        Res.string.shift_preset_afternoon, Res.string.shift_preset_afternoon_acronym,
        LocalTime(15, 0), LocalTime(22, 0), selectedByDefault = true, color = "#FB8C00",
    ),
    Night(
        Res.string.shift_preset_night, Res.string.shift_preset_night_acronym,
        LocalTime(22, 0), LocalTime(8, 0), selectedByDefault = true, color = "#3949AB",
    ),
    MorningAfternoon(
        Res.string.shift_preset_morning_afternoon, Res.string.shift_preset_morning_afternoon_acronym,
        LocalTime(8, 0), LocalTime(22, 0), selectedByDefault = false, color = "#00897B",
    ),
    Duty24(
        Res.string.shift_preset_duty24, Res.string.shift_preset_duty24_acronym,
        LocalTime(8, 0), LocalTime(8, 0), selectedByDefault = false, color = "#E53935",
    ),
}
