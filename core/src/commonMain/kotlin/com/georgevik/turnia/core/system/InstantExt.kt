package com.georgevik.turnia.core.system

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun Instant.toYearMonth(): YearMonth = toLocalDateTime(TimeZone.currentSystemDefault()).let {
    YearMonth(it.year, it.month)
}

fun Instant.toLocalDate(): LocalDate = toLocalDateTime(TimeZone.currentSystemDefault()).let {
    LocalDate(it.year, it.month, it.day)
}


fun LocalDate.toInstant() = atStartOfDayIn(TimeZone.currentSystemDefault())
