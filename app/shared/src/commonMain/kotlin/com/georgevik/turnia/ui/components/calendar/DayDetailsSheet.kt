package com.georgevik.turnia.ui.components.calendar

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.components.calendar.animtransition.CalendarSheetBoundsTransform
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_month_year
import turnia.app.shared.generated.resources.calendar_months
import turnia.app.shared.generated.resources.event_add
import turnia.app.shared.generated.resources.event_details_count
import turnia.app.shared.generated.resources.event_details_empty
import turnia.app.shared.generated.resources.event_manage
import turnia.app.shared.generated.resources.event_status_active
import turnia.app.shared.generated.resources.event_status_on_sale

/** Top corner radius the tile animates towards as it becomes the sheet. */
private val SheetCornerRadius = 28.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DayDetailsSheet(
    animatedVisibilityScope: AnimatedVisibilityScope,
    date: LocalDate,
    events: List<CalendarEventUi>,
    onAddEvent: () -> Unit,
    onManageEvent: (CalendarEventUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .sharedBounds(
                sharedContentState = rememberSharedContentState(calendarContainerKey(date)),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = CalendarSheetBoundsTransform,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            ),
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 24.dp),
        ) {
            // Drag handle — fades in with the rest of the sheet chrome.
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .fadeInContent(animatedVisibilityScope),
            )

            SheetHeader(
                animatedVisibilityScope = animatedVisibilityScope,
                date = date,
                eventCount = events.size,
                onAddEvent = onAddEvent,
            )

            Spacer(Modifier.height(16.dp))

            Box(Modifier.fadeInContent(animatedVisibilityScope)) {
                if (events.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.event_details_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    Column {
                        events.forEachIndexed { index, event ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            EventDetailCard(event = event, onManage = { onManageEvent(event) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.SheetHeader(
    animatedVisibilityScope: AnimatedVisibilityScope,
    date: LocalDate,
    eventCount: Int,
    onAddEvent: () -> Unit,
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
            onClick = onAddEvent,
            modifier = Modifier.fadeInContent(animatedVisibilityScope),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.event_add),
            )
        }
    }
}

private fun Modifier.fadeInContent(scope: AnimatedVisibilityScope): Modifier =
    this.then(
        with(scope) {
            Modifier.animateEnterExit(
                enter = fadeIn(),
                exit = fadeOut(),
            )
        },
    )

@Composable
private fun EventDetailCard(
    event: CalendarEventUi,
    onManage: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Accent bar in the event's color.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(event.background),
            )
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TypeChip(event.text)
                    StatusChip(onSale = event.onSale)
                    Spacer(Modifier.weight(1f))
                    if (event.isOwner) {
                        FilledTonalButton(
                            onClick = onManage,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(stringResource(Res.string.event_manage))
                        }
                    }
                }

                event.timeRange?.let { time ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (event.isOwner) Icons.Default.Person else Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusChip(onSale: Boolean) {
    if (onSale) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(Res.string.event_status_on_sale),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = stringResource(Res.string.event_status_active),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
