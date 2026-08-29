package com.georgevik.turnia.ui.components.daydetail.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.components.calendar.animtransition.CalendarSheetBoundsTransform
import com.georgevik.turnia.ui.components.calendar.calendarNumberKey
import com.georgevik.turnia.ui.components.daydetail.animation.fadeInContent
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_month_year
import turnia.app.shared.generated.resources.calendar_months
import turnia.app.shared.generated.resources.event_add
import turnia.app.shared.generated.resources.event_add_close
import turnia.app.shared.generated.resources.event_details_count

@Composable
fun SharedTransitionScope.DayDetailHeader(
    animatedVisibilityScope: AnimatedVisibilityScope,
    date: LocalDate,
    eventCount: Int,
    adding: Boolean,
    onToggleAdd: () -> Unit,
) {
    val monthNames = stringArrayResource(Res.array.calendar_months)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = date.day.toString(),
            modifier = Modifier
                .sharedElement(
                    sharedContentState = rememberSharedContentState(calendarNumberKey(date)),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = CalendarSheetBoundsTransform,
                )
                .skipToLookaheadSize(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fadeInContent(animatedVisibilityScope),
        ) {
            Text(
                text = stringResource(
                    Res.string.calendar_month_year,
                    monthNames[date.month.ordinal],
                    date.year,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.event_details_count, eventCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(
            onClick = onToggleAdd,
            modifier = Modifier.fadeInContent(animatedVisibilityScope),
        ) {
            Icon(
                imageVector = if (adding) Icons.Default.Close else Icons.Default.Add,
                contentDescription = stringResource(
                    if (adding) Res.string.event_add_close else Res.string.event_add,
                ),
            )
        }
    }
}
