package com.geoviksoft.turnia.ui.system.components.time

import kotlin.test.Test
import kotlin.test.assertEquals

class NextDayMarkTest {

    @Test
    fun aDayShiftEndsTheSameDay() = assertEquals("", nextDayMark("08:00", "15:00"))

    @Test
    fun aNightEndsTheNextDay() = assertEquals(" +1", nextDayMark("22:00", "08:00"))

    @Test
    fun twentyFourHoursEndTheNextDay() = assertEquals(" +1", nextDayMark("08:00", "08:00"))
}
