package com.georgevik.turnia.ui.components.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.components.calendar.animtransition.CALENDAR_TRANSITION_MILLIS
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CalendarViewer(
    modifier: Modifier = Modifier,
    theme: CalendarTheme = CalendarThemes.primary(),
    eventsByDate: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
    onAddEvent: (LocalDate) -> Unit = {},
    onManageEvent: (CalendarEventUi) -> Unit = {},
) {
    // Today is resolved here and always highlighted in the grid.
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    // Always work from the first day of the displayed month.
    var month by remember { mutableStateOf(LocalDate(today.year, today.month, 1)) }
    // The tapped day drives both selection highlight and the sheet transition.
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var sheetDate by remember { mutableStateOf<LocalDate?>(null) }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Keep the top (status bar) inset, but only a small horizontal margin.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CalendarMonthHeader(month, { month = it })

                CalendarWeekTitles()

                CalendarGrid(
                    month = month,
                    calendarTheme = theme,
                    selectedDate = selectedDate,
                    eventsByDate = eventsByDate,
                    sharedScope = this@SharedTransitionLayout,
                    onDateSelected = { date ->
                        sheetDate = date
                        selectedDate = date
                    },
                    onMonthChanged = { newMonth -> month = newMonth }
                )
            }

            BottomSheetShadow(visible = selectedDate != null, onClick = { selectedDate = null })

            // BottomSheet
            AnimatedVisibility(
                visible = selectedDate != null,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                sheetDate?.let { date ->
                    DayDetailsSheet(
                        animatedVisibilityScope = this,
                        date = date,
                        events = eventsByDate[date].orEmpty(),
                        onAddEvent = { onAddEvent(date) },
                        onManageEvent = onManageEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthHeader(monthDate: LocalDate, onMonthChange: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val monthNames = stringArrayResource(Res.array.calendar_months)
        Text(
            text = stringResource(
                Res.string.calendar_month_year,
                monthNames[monthDate.month.ordinal],
                monthDate.year,
            ),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Row(modifier = Modifier.wrapContentSize()) {
            IconButton(onClick = { onMonthChange(monthDate.minus(1, DateTimeUnit.MONTH)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(Res.string.calendar_previous_month),
                )
            }
            IconButton(onClick = { onMonthChange(monthDate.plus(1, DateTimeUnit.MONTH)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(Res.string.calendar_next_month),
                )
            }
        }
    }
}

@Composable
private fun CalendarWeekTitles() {
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
}

@Composable
private fun ColumnScope.CalendarGrid(
    month: LocalDate,
    calendarTheme: CalendarTheme,
    selectedDate: LocalDate?,
    eventsByDate: Map<LocalDate, List<CalendarEventUi>>,
    sharedScope: SharedTransitionScope,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
) {
    // Monday-first grid. Leading days come from the previous month; the grid
    // spans a fixed number of weeks so every cell is the same size.
    val leadingDays = month.dayOfWeek.ordinal
    val gridStart = month.minus(leadingDays, DateTimeUnit.DAY)
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        // Cells are uniform, so how many event rows fit is computed once here
        // (rather than measuring every cell) and passed down.
        val cellHeight = maxHeight / WEEKS
        val eventArea = cellHeight - CalendarCellNumberHeight - 12.dp
        val maxEventRows =
            (eventArea / CalendarEventSlotHeight).toInt().coerceAtLeast(0)

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
                        val date =
                            gridStart.plus(week * 7 + dayOfWeek, DateTimeUnit.DAY)
                        val dateInMonth =
                            date.month == month.month && date.year == month.year
                        CalendarCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            inMonth = dateInMonth,
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            theme = calendarTheme,
                            events = eventsByDate[date].orEmpty(),
                            maxEventRows = maxEventRows,
                            sharedScope = sharedScope,
                            isExpanded = date == selectedDate,
                            onClick = {
                                if (!dateInMonth) {
                                    // Jump to the tapped day's month first.
                                    onMonthChanged(LocalDate(date.year, date.month, 1))
                                }
                                onDateSelected(date)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetShadow(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(CALENDAR_TRANSITION_MILLIS)),
        exit = fadeOut(tween(CALENDAR_TRANSITION_MILLIS)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}
