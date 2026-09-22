package com.geoviksoft.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime

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
)

/** The one-off event being written in the day sheet's add pane. */
@Immutable
data class OneOffEventFormUi(
    val name: String = "",
    val notes: String = "",
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean = false,
    val color: Color,
    /** The event being edited, or null while a new one is being written. */
    val editingId: String? = null,
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
    data class Edit(val eventId: String) : OneOffFormAction
    data object Cancel : OneOffFormAction
    data object Save : OneOffFormAction
    /** Deletes the event being edited. The sheet asks first, and only a confirmed one arrives. */
    data object Delete : OneOffFormAction
    data class NameChanged(val name: String) : OneOffFormAction
    data class NotesChanged(val notes: String) : OneOffFormAction
    data class StartChanged(val start: LocalDateTime) : OneOffFormAction
    data class EndChanged(val end: LocalDateTime) : OneOffFormAction
    data class AllDayChanged(val allDay: Boolean) : OneOffFormAction
    data class ColorPicked(val color: Color) : OneOffFormAction
}

/** A one-off event write that failed, shown until the sheet says it has been. */
enum class OneOffEventMessage { SaveFailed, DeleteFailed }
