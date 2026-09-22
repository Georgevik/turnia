package com.geoviksoft.turnia.ui.components.daydetail.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_months_short
import turnia.app.shared.generated.resources.one_off_event_cancel
import turnia.app.shared.generated.resources.one_off_event_picker_ok

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Material has no date-and-time picker, so this is its two pickers in a row: the day first, then
 * the time on it. Dismissing either one keeps the value it started with. Without [pickTime] it
 * stops at the day and keeps the time [initial] had.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
    pickTime: Boolean = true,
) {
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val ok = stringResource(Res.string.one_off_event_picker_ok)
    val cancel = stringResource(Res.string.one_off_event_cancel)

    val date = pickedDate
    if (date == null) {
        // The picker speaks UTC midnight, whatever the device's zone: a day, not an instant.
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = initial.date.toEpochDays() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = dateState.selectedDateMillis
                            ?.let { LocalDate.fromEpochDays(it / MILLIS_PER_DAY) }
                            ?: initial.date
                        if (pickTime) pickedDate = picked else onConfirm(LocalDateTime(picked, initial.time))
                    },
                ) { Text(ok) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(cancel) } },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(LocalDateTime(date, LocalTime(timeState.hour, timeState.minute))) },
                ) { Text(ok) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(cancel) } },
            text = { TimePicker(state = timeState) },
        )
    }
}

/** "3 Oct · 09:00" — the day, not the year: a one-off event is almost always close by. */
@Composable
fun LocalDateTime.dateTimeLabel(): String = "${date.dateLabel()} · ${time.clockLabel()}"

/** "3 Oct" — what an all-day event shows where a timed one shows its time. */
@Composable
fun LocalDate.dateLabel(): String {
    val months = stringArrayResource(Res.array.calendar_months_short)
    return "$day ${months[month.ordinal]}"
}

fun LocalTime.clockLabel(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
