package com.geoviksoft.turnia.ui.shiftsetup.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ShiftPresetTest {

    @Test
    fun aNewGroupIsProposedTheFourUsualShiftsInOrder() {
        assertEquals(
            listOf(
                ShiftPreset.Morning,
                ShiftPreset.Afternoon,
                ShiftPreset.Night,
                ShiftPreset.MorningAfternoon,
            ),
            ShiftPreset.groupDefaults,
        )
    }
}
