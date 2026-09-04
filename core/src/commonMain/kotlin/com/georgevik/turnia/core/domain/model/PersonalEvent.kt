package com.georgevik.turnia.core.domain.model

import com.georgevik.turnia.core.system.toLocalDate
import kotlin.time.Instant

data class PersonalEvent(
    val id: EventId,
    val type: PersonalEventType,
    val date: Instant,
    val notes: String?,
) {
    val localDate = date.toLocalDate()
}
