package com.georgevik.turnia.ui.components.calendar.daydetail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.components.calendar.animtransition.CalendarSheetBoundsTransform
import com.georgevik.turnia.ui.components.calendar.calendarContainerKey
import com.georgevik.turnia.ui.components.calendar.daydetail.animation.fadeInContent
import com.georgevik.turnia.ui.components.calendar.daydetail.model.PredefinedEventUi
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_details_empty

/** Top corner radius the tile animates towards as it becomes the sheet. */
private val SheetCornerRadius = 28.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DayDetailsSheet(
    animatedVisibilityScope: AnimatedVisibilityScope,
    date: LocalDate,
    events: List<CalendarEventUi>,
    predefinedEvents: List<PredefinedEventUi>,
    onPickPredefined: (PredefinedEventUi) -> Unit,
    onAddCustom: () -> Unit,
    onManageEvent: (CalendarEventUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by remember { mutableStateOf(false) }
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

            DayDetailHeader(
                animatedVisibilityScope = animatedVisibilityScope,
                date = date,
                eventCount = events.size,
                adding = adding,
                onToggleAdd = { adding = !adding },
            )

            Spacer(Modifier.height(16.dp))

            Box(Modifier.fadeInContent(animatedVisibilityScope)) {
                if (adding) {
                    DayDetailAddEvent(
                        predefinedEvents = predefinedEvents,
                        onPickPredefined = onPickPredefined,
                        onAddCustom = onAddCustom,
                    )
                } else if (events.isEmpty()) {
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
                            DayEventRow(event = event, onManage = { onManageEvent(event) })
                        }
                    }
                }
            }
        }
    }
}
