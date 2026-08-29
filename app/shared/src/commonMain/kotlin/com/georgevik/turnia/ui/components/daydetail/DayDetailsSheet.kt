package com.georgevik.turnia.ui.components.daydetail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.ui.components.calendar.animtransition.CalendarSheetBoundsTransform
import com.georgevik.turnia.ui.components.calendar.calendarContainerKey
import com.georgevik.turnia.ui.components.daydetail.animation.fadeInContent
import com.georgevik.turnia.ui.components.daydetail.components.DayDetailAddEvent
import com.georgevik.turnia.ui.components.daydetail.components.DayDetailHeader
import com.georgevik.turnia.ui.components.daydetail.components.DayEventRow
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_details_empty

private val SheetCornerRadius = 28.dp

private const val DragDismissThresholdPx = 120f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DayDetailsSheet(
    animatedVisibilityScope: AnimatedVisibilityScope,
    date: LocalDate,
    events: List<CalendarEventUi>,
    openEditTypeScreen: (groupId: String, groupName: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailsSheetViewModel = koinViewModel(key = date.toString()) {
        parametersOf(date)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DayDetailsSheetUiEvent.EditGroup ->
                    openEditTypeScreen(event.groupId, event.groupName)
            }
        }
    }
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
                .padding(bottom = 16.dp),
        ) {
            var dragOffset by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset > DragDismissThresholdPx) onClose()
                                dragOffset = 0f
                            },
                            onDragCancel = { dragOffset = 0f },
                            onVerticalDrag = { _, delta -> dragOffset += delta },
                        )
                    }
                    .padding(vertical = 10.dp, horizontal = 24.dp)
                    .fadeInContent(animatedVisibilityScope),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
            }

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
                        sections = uiState.predefinedSections,
                        onPickPredefined = { predefined ->
                            viewModel.addPredefinedEvent(predefined.id)
                            onClose()
                        },
                        onEditGroup = { groupId, groupName ->
                            viewModel.editGroup(groupId, groupName)
                        },
                        onAddCustom = {
                            viewModel.addCustomEvent()
                            onClose()
                        },
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
                            DayEventRow(event = event)
                        }
                    }
                }
            }
        }
    }
}
