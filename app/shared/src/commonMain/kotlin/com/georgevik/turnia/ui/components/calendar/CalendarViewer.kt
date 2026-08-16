package com.georgevik.turnia.ui.components.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_month_year
import turnia.app.shared.generated.resources.calendar_months
import turnia.app.shared.generated.resources.calendar_next_month
import turnia.app.shared.generated.resources.calendar_previous_month
import turnia.app.shared.generated.resources.calendar_weekday_initials
import kotlin.time.Clock

private const val WEEKS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarViewer(
    modifier: Modifier = Modifier.Companion,
    theme: CalendarTheme = CalendarThemes.primary(),
    eventsByDate: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
    onAddEvent: (LocalDate) -> Unit = {},
    onManageEvent: (CalendarEventUi) -> Unit = {},
) {
    // Today is resolved here and always highlighted in the grid.
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    // Always work from the first day of the displayed month.
    var month by remember { mutableStateOf(LocalDate(today.year, today.month, 1)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Keep the top (status bar) inset, but only a small horizontal margin.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val monthNames = stringArrayResource(Res.array.calendar_months)
            Text(
                text = stringResource(
                    Res.string.calendar_month_year,
                    monthNames[month.month.ordinal],
                    month.year,
                ),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Row(modifier = Modifier.wrapContentSize()) {
                IconButton(onClick = { month = month.minus(1, DateTimeUnit.MONTH) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.calendar_previous_month),
                    )
                }
                IconButton(onClick = { month = month.plus(1, DateTimeUnit.MONTH) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(Res.string.calendar_next_month),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            stringArrayResource(Res.array.calendar_weekday_initials).forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Monday-first grid. Leading days come from the previous month; the grid
        // spans a fixed number of weeks so every cell is the same size.
        val leadingDays = month.dayOfWeek.ordinal
        val gridStart = month.minus(leadingDays, DateTimeUnit.DAY)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Cells are uniform, so how many event rows fit is computed once here
            // (rather than measuring every cell) and passed down.
            val cellHeight = maxHeight / WEEKS
            val eventArea = cellHeight - CalendarCellNumberHeight - 12.dp
            val maxEventRows = (eventArea / CalendarEventSlotHeight).toInt().coerceAtLeast(0)

            // A Column of weighted Rows keeps all cells equal and, unlike the
            // experimental Grid, never lets a long event name stretch a column.
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(WEEKS) { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        repeat(7) { dayOfWeek ->
                            val date = gridStart.plus(week * 7 + dayOfWeek, DateTimeUnit.DAY)
                            val dateInMonth =
                                date.month == month.month && date.year == month.year
                            CalendarCell(
                                modifier = Modifier.weight(1f),
                                date = date,
                                inMonth = dateInMonth,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                theme = theme,
                                events = eventsByDate[date].orEmpty(),
                                maxEventRows = maxEventRows,
                                onClick = {
                                    if (!dateInMonth) {
                                        // Jump to the tapped day's month first.
                                        month = LocalDate(date.year, date.month, 1)
                                    }
                                    // Select the day and open its details sheet.
                                    selectedDate = date
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Details of the selected day, shown in a modal bottom sheet.
    selectedDate?.let { date ->
        ModalBottomSheet(
            onDismissRequest = { selectedDate = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            EventDetailsSheetContent(
                date = date,
                events = eventsByDate[date].orEmpty(),
                onAddEvent = { onAddEvent(date) },
                onManageEvent = onManageEvent,
            )
        }
    }
}
