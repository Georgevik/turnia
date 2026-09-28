package com.geoviksoft.turnia.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RetentionWindowTest {

    @Test
    fun startsOneMonthBeforeToday() {
        assertEquals(LocalDate(2026, 9, 15), RetentionWindow.start(LocalDate(2026, 10, 15)))
    }

    @Test
    fun clampsToTheEndOfAShorterMonth() {
        assertEquals(LocalDate(2027, 2, 28), RetentionWindow.start(LocalDate(2027, 3, 31)))
        assertEquals(LocalDate(2028, 2, 29), RetentionWindow.start(LocalDate(2028, 3, 31)))
    }
}
