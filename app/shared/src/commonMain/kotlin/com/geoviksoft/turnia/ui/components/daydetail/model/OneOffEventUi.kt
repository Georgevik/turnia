package com.geoviksoft.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import com.geoviksoft.turnia.ui.system.color.toHex
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

/** A one-off event as a row in a day's sheet. */
@Immutable
data class OneOffEventUi(
    val id: String,
    val name: String,
    val notes: String?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val color: Color,
) {
    fun toDomain() = PersonalOneOffEvent(
        id = EventId(id),
        name = name,
        notes = notes,
        start = start,
        end = end,
        allDay = allDay,
        color = color.toHex(),
    )
}

fun PersonalOneOffEvent.toUi() = OneOffEventUi(
    id = id.value,
    name = name,
    notes = notes,
    start = start,
    end = end,
    allDay = allDay,
    color = color.toComposeColorOr(entityColor(id.value)),
)

/** Every day each event covers, so one spanning several days is listed on each of them. */
fun List<PersonalOneOffEvent>.toUiByDate(): Map<LocalDate, List<OneOffEventUi>> = buildMap<LocalDate, MutableList<OneOffEventUi>> {
    this@toUiByDate.sortedBy { it.start }.forEach { event ->
        val ui = event.toUi()
        var day = event.start.date
        while (day <= event.end.date) {
            getOrPut(day) { mutableListOf() }.add(ui)
            day = day.plus(1, DateTimeUnit.DAY)
        }
    }
}

/** The one-off event being written in the day sheet's add pane. */
@Immutable
data class OneOffEventFormUi(
    val name: String = "",
    val notes: String = "",
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean = false,
    val color: Color,
    /** The event being edited, as it was before, or null while a new one is being written. */
    val editing: OneOffEventUi? = null,
) {
    /**
     * An all-day event keeps the times it had underneath, so switching back restores them, but
     * only its days count.
     */
    val endsBeforeStart: Boolean get() = if (allDay) end.date < start.date else end < start

    val canSave: Boolean get() = name.isNotBlank() && !endsBeforeStart
}

/** What the user does to the one-off form, so the whole of it travels as one callback. */
sealed interface OneOffFormAction {
    data object Open : OneOffFormAction
    data class Edit(val event: OneOffEventUi) : OneOffFormAction
    data object Cancel : OneOffFormAction
    data object Save : OneOffFormAction
    /** Deletes the event being edited. The sheet asks first, and only a confirmed one arrives. */
    data object Delete : OneOffFormAction
    /** Makes a personal type out of the event being edited, which stays as it is. */
    data object SaveAsShift : OneOffFormAction
    data class NameChanged(val name: String) : OneOffFormAction
    data class NotesChanged(val notes: String) : OneOffFormAction
    data class StartChanged(val start: LocalDateTime) : OneOffFormAction
    data class EndChanged(val end: LocalDateTime) : OneOffFormAction
    data class AllDayChanged(val allDay: Boolean) : OneOffFormAction
    data class ColorPicked(val color: Color) : OneOffFormAction
}

/** A one-off event write that failed, shown until the sheet says it has been. */
enum class OneOffEventMessage { SaveFailed, DeleteFailed }
