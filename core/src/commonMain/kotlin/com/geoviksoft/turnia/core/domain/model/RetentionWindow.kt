package com.geoviksoft.turnia.core.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * The recent stretch of the calendar the server keeps. Anything dated before [start] is eligible for
 * the server's purge, and only the devices that synced it still have it.
 */
object RetentionWindow {

    fun start(today: LocalDate): LocalDate = today.minus(1, DateTimeUnit.MONTH)
}
