package com.georgevik.turnia.ui.components.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_month_year
import turnia.app.shared.generated.resources.calendar_months
import turnia.app.shared.generated.resources.calendar_next_month
import turnia.app.shared.generated.resources.calendar_previous_month
import turnia.app.shared.generated.resources.calendar_weekday_initials

@OptIn(ExperimentalGridApi::class)
@Composable
fun CalendarViewer(
    modifier: Modifier = Modifier.Companion,
    theme: CalendarTheme = CalendarThemes.primary(),
) {
    // Today is resolved here and always highlighted in the grid.
//    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val today = remember { LocalDate(2023, 10, 1) }
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
            modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.headlineMedium,
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

        // Monday-first grid: how many leading days from the previous month, and
        // how many whole weeks (5–6) this month spans.
        val leadingDays = month.dayOfWeek.ordinal
        val weeks = 6
        val gridStart = month.minus(leadingDays, DateTimeUnit.DAY)

        Grid(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            config = {
                repeat(7) { column(1.fr) }
                repeat(weeks) { row(1.fr) }
            },
        ) {
            repeat(weeks * 7) { index ->
                val date = gridStart.plus(index, DateTimeUnit.DAY)
                val dateInMonth = date.month == month.month && date.year == month.year
                CalendarCell(
                    date = date,
                    inMonth = dateInMonth,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    theme = theme,
                    onClick = {
                        if (dateInMonth) {
                            // Toggle selection for days in the displayed month.
                            selectedDate = if (selectedDate == date) null else date
                        } else {
                            // Jump to the tapped day's month and select it.
                            month = LocalDate(date.year, date.month, 1)
                            selectedDate = date
                        }
                    },
                )
            }
        }
    }
}
