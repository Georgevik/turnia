package com.geoviksoft.turnia.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * An event that belongs to one user and no group: either a [PersonalTypedEvent], an instance of
 * one of their [PersonalEventType]s, or a [PersonalOneOffEvent], which carries its own name,
 * colour and times.
 */
sealed interface PersonalEvent {
    val id: EventId
}

data class PersonalTypedEvent(
    override val id: EventId,
    val type: PersonalEventType,
    val date: LocalDate,
    val notes: String?,
) : PersonalEvent

data class PersonalOneOffEvent(
    override val id: EventId,
    val name: String,
    val notes: String?,
    val dateStart: LocalDate,
    val dateEnd: LocalDate,
    val timeStart: LocalTime,
    val timeEnd: LocalTime,
    val color: String,
) : PersonalEvent
