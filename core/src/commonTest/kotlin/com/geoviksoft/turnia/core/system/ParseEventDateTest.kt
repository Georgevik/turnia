package com.geoviksoft.turnia.core.system

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseEventDateTest {

    @Test
    fun readsTheStoredDay() {
        assertEquals(LocalDate(2026, 9, 24), parseEventDate("2026-09-24"))
    }

    @Test
    fun readsALegacyInstantAsItsLocalDay() {
        // Noon UTC is the same calendar day in every time zone from UTC-11 to UTC+11.
        assertEquals(LocalDate(2026, 9, 24), parseEventDate("2026-09-24T12:00:00Z"))
    }
}
