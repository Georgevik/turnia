package com.geoviksoft.turnia.ui.shiftsetup.model

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShiftSetupUiTest {

    private fun row(selected: Boolean) = ShiftRowUi(
        id = "morning",
        preset = ShiftPreset.Morning,
        name = "Morning",
        acronym = "M",
        start = "08:00",
        end = "15:00",
        color = Color.Red,
        selected = selected,
    )

    @Test
    fun aSelectionNotSavingAndNotClosedCanConfirm() {
        val state = ShiftSetupUi(rows = listOf(row(selected = true)), saving = false, closed = false)

        assertTrue(state.canConfirm)
    }

    @Test
    fun aClosedSetupCannotBeConfirmedAgain() {
        val state = ShiftSetupUi(rows = listOf(row(selected = true)), saving = false, closed = true)

        assertFalse(state.canConfirm)
    }
}
