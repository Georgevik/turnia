package com.geoviksoft.turnia.ui.main.swap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.ui.components.calendar.model.CalendarEventUi
import com.geoviksoft.turnia.ui.components.daydetail.components.DayEventRow
import com.geoviksoft.turnia.ui.main.swap.components.SwapGroupFilterSheet
import com.geoviksoft.turnia.ui.main.swap.model.SwapMessage
import com.geoviksoft.turnia.ui.main.swap.model.SwapSegment
import com.geoviksoft.turnia.ui.main.swap.model.SwapUi
import com.geoviksoft.turnia.ui.main.system.EmptyState
import com.geoviksoft.turnia.ui.main.system.ScreenHeader
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_swap_error_not_found
import turnia.app.shared.generated.resources.event_swap_error_not_member
import turnia.app.shared.generated.resources.event_swap_error_not_swappable
import turnia.app.shared.generated.resources.event_swap_error_own_shift
import turnia.app.shared.generated.resources.event_swap_error_save
import turnia.app.shared.generated.resources.event_swap_error_taken_by_someone
import turnia.app.shared.generated.resources.event_swap_take_cancel
import turnia.app.shared.generated.resources.event_swap_take_confirm
import turnia.app.shared.generated.resources.event_swap_take_confirm_body
import turnia.app.shared.generated.resources.event_swap_take_confirm_title
import turnia.app.shared.generated.resources.swap_empty_available_body
import turnia.app.shared.generated.resources.swap_empty_available_title
import turnia.app.shared.generated.resources.swap_empty_covered_body
import turnia.app.shared.generated.resources.swap_empty_covered_title
import turnia.app.shared.generated.resources.swap_empty_covering_body
import turnia.app.shared.generated.resources.swap_empty_covering_title
import turnia.app.shared.generated.resources.swap_empty_offered_body
import turnia.app.shared.generated.resources.swap_empty_offered_title
import turnia.app.shared.generated.resources.swap_filter_title
import turnia.app.shared.generated.resources.swap_segment_available
import turnia.app.shared.generated.resources.swap_segment_covered
import turnia.app.shared.generated.resources.swap_segment_covering
import turnia.app.shared.generated.resources.swap_segment_offered
import turnia.app.shared.generated.resources.swap_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(viewModel: SwapViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterOpen by remember { mutableStateOf(false) }
    var pendingTake by remember { mutableStateOf<CalendarEventUi?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val state = uiState) {
            SwapUi.Loading -> ScreenHeader(stringResource(Res.string.swap_title))

            is SwapUi.Success -> {
                // ScreenHeader has no trailing slot of its own, so the filter sits beside it here
                // rather than changing a component four other screens share.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenHeader(
                        title = stringResource(Res.string.swap_title),
                        modifier = Modifier.weight(1f),
                    )
                    if (state.filterable) {
                        FilterButton(
                            hidden = state.groups.count { !it.selected },
                            onClick = { filterOpen = true },
                        )
                    }
                }

                SecondaryScrollableTabRow(
                    selectedTabIndex = SwapSegment.entries.indexOf(state.segment),
                    edgePadding = 0.dp,
                ) {
                    SwapSegment.entries.forEach { segment ->
                        Tab(
                            selected = segment == state.segment,
                            onClick = { viewModel.segmentSelected(segment) },
                            text = { Text(stringResource(segment.title())) },
                        )
                    }
                }

                SwapMessageSnackbar(state.userMessage, viewModel::userMessageShown)

                if (state.events.isEmpty()) {
                    val (title, body) = state.segment.emptyState()
                    EmptyState(
                        icon = Icons.Default.SwapHoriz,
                        title = stringResource(title),
                        body = stringResource(body),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.events, key = { it.id.value }) { event ->
                            SwapEventRow(
                                event = event,
                                onTake = if (event.canTake) {
                                    { pendingTake = event }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }

                if (filterOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { filterOpen = false },
                        sheetState = sheetState,
                    ) {
                        SwapGroupFilterSheet(
                            groups = state.groups,
                            onToggle = viewModel::groupToggled,
                            onSelectAll = viewModel::allGroupsSelected,
                        )
                    }
                }
            }
        }
    }

    pendingTake?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingTake = null },
            title = { Text(stringResource(Res.string.event_swap_take_confirm_title)) },
            text = { Text(stringResource(Res.string.event_swap_take_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.takeEvent(event)
                    pendingTake = null
                }) {
                    Text(stringResource(Res.string.event_swap_take_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTake = null }) {
                    Text(stringResource(Res.string.event_swap_take_cancel))
                }
            },
        )
    }
}

/**
 * A row here is a day's row taken out of its day, so it has to say which day it is: the list spans
 * three months and the date is the first thing anyone looks for.
 */
@Composable
private fun SwapEventRow(event: CalendarEventUi, onTake: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = event.date.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DayEventRow(event = event, onTake = onTake)
    }
}

@Composable
private fun FilterButton(hidden: Int, onClick: () -> Unit) {
    val description = stringResource(Res.string.swap_filter_title)
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (hidden > 0) Badge { Text(hidden.toString()) } }) {
            Icon(imageVector = Icons.Default.FilterList, contentDescription = description)
        }
    }
}

@Composable
private fun SwapMessageSnackbar(message: SwapMessage?, onShown: () -> Unit) {
    val snackbar = LocalSnackbar.current
    message?.let {
        val text = stringResource(it.resource())
        LaunchedEffect(it) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            onShown()
        }
    }
}

private fun SwapSegment.title(): StringResource = when (this) {
    SwapSegment.OFFERED -> Res.string.swap_segment_offered
    SwapSegment.AVAILABLE -> Res.string.swap_segment_available
    SwapSegment.COVERED -> Res.string.swap_segment_covered
    SwapSegment.COVERING -> Res.string.swap_segment_covering
}

private fun SwapSegment.emptyState(): Pair<StringResource, StringResource> = when (this) {
    SwapSegment.OFFERED ->
        Res.string.swap_empty_offered_title to Res.string.swap_empty_offered_body

    SwapSegment.AVAILABLE ->
        Res.string.swap_empty_available_title to Res.string.swap_empty_available_body

    SwapSegment.COVERED ->
        Res.string.swap_empty_covered_title to Res.string.swap_empty_covered_body

    SwapSegment.COVERING ->
        Res.string.swap_empty_covering_title to Res.string.swap_empty_covering_body
}

private fun SwapMessage.resource(): StringResource = when (this) {
    SwapMessage.NotSwappable -> Res.string.event_swap_error_not_swappable
    SwapMessage.NotMember -> Res.string.event_swap_error_not_member
    SwapMessage.OwnShift -> Res.string.event_swap_error_own_shift
    SwapMessage.NotFound -> Res.string.event_swap_error_not_found
    SwapMessage.TakenBySomeoneElse -> Res.string.event_swap_error_taken_by_someone
    SwapMessage.SaveFailed -> Res.string.event_swap_error_save
}
