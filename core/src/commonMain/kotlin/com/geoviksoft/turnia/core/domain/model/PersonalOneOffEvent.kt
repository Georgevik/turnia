package com.geoviksoft.turnia.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class PersonalOneOffEvent(
    val id: EventId,
    val name: String,
    val notes: String?,
    val dateStart: LocalDate,
    val dateEnd: LocalDate,
    val timeStart: LocalTime,
    val timeEnd: LocalTime,
    val color: String,
)
