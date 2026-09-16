package com.geoviksoft.turnia.core.domain.model

import kotlinx.datetime.LocalDate

data class PersonalEvent(
    val id: EventId,
    val type: PersonalEventType,
    /** A day, not an instant: the same one on every device, whatever its time zone. */
    val date: LocalDate,
    val notes: String?,
)
