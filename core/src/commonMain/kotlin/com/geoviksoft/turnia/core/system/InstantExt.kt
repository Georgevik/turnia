package com.geoviksoft.turnia.core.system

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
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

fun Instant.toTimestamp(): Timestamp = Timestamp(
    seconds = this.epochSeconds,
    nanoseconds = this.nanosecondsOfSecond
)

fun LocalDate.toInstant() = atStartOfDayIn(TimeZone.currentSystemDefault())

/**
 * A personal event's day, as stored: `YYYY-MM-DD`. Events written for a while carried the local
 * midnight as a UTC instant instead; those are read back as the day of that instant here, which is
 * the day their author picked whenever this device shares their time zone.
 */
fun parseEventDate(value: String): LocalDate =
    Instant.parseOrNull(value)?.toLocalDate() ?: LocalDate.parse(value)

fun BaseTimestamp?.toInstantOrNull(): Instant? = when (this) {
    is Timestamp -> this.toInstant()
    else -> null
}

fun Timestamp.toInstant(): Instant = Instant.fromEpochSeconds(seconds, nanoseconds)
