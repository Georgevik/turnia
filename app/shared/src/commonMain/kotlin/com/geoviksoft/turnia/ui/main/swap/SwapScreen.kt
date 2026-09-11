package com.geoviksoft.turnia.ui.main.swap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.main.swap.components.SwapEventRow
import com.geoviksoft.turnia.ui.main.swap.components.SwapGroupFilterSheet
import com.geoviksoft.turnia.ui.main.swap.components.SwapSegmentChips
import com.geoviksoft.turnia.ui.main.swap.model.SwapMessage
import com.geoviksoft.turnia.ui.main.swap.model.SwapRowUi
import com.geoviksoft.turnia.ui.main.swap.model.SwapSegment
import com.geoviksoft.turnia.ui.main.swap.model.SwapUi
import com.geoviksoft.turnia.ui.main.system.EmptyState
import com.geoviksoft.turnia.ui.main.system.ScreenHeader
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_month_year
import turnia.app.shared.generated.resources.calendar_months
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
import turnia.app.shared.generated.resources.swap_empty_colleagues_body
import turnia.app.shared.generated.resources.swap_empty_colleagues_title
import turnia.app.shared.generated.resources.swap_empty_mine_body
import turnia.app.shared.generated.resources.swap_empty_mine_title
import turnia.app.shared.generated.resources.swap_empty_uncovered_body
import turnia.app.shared.generated.resources.swap_empty_uncovered_title
import turnia.app.shared.generated.resources.swap_filter_title
import turnia.app.shared.generated.resources.swap_filter_uncovered
import turnia.app.shared.generated.resources.swap_title
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(viewModel: SwapViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterOpen by remember { mutableStateOf(false) }
    var pendingTake by remember { mutableStateOf<DayEventUi?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // The tab bar below already stands clear of the gesture area: taking the bottom inset again
    // would leave a gap between the list and the bar.
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            when (val state = uiState) {
                SwapUi.Loading -> Header()

                is SwapUi.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Header(modifier = Modifier.weight(1f),)
                        if (state.filterable) {
                            FilterButton(
                                hidden = state.groups.count { !it.selected },
                                onClick = { filterOpen = true },
                            )
                        }
                    }

                    SwapSegmentChips(
                        selected = state.segment,
                        onSelected = viewModel::segmentSelected,
                    )

                    // Kept while it is on, even with nothing covered left, so it can be turned off.
                    val mine = state.segment == SwapSegment.MINE
                    if (mine && (state.hasCovered || state.onlyUncovered)) {
                        UncoveredFilterChip(
                            selected = state.onlyUncovered,
                            onClick = viewModel::onlyUncoveredToggled,
                        )
                    }

                    SwapMessageSnackbar(state.userMessage, viewModel::userMessageShown)

                    if (state.rows.isEmpty()) {
                        val (title, body) = if (mine && state.onlyUncovered && state.hasCovered) {
                            Res.string.swap_empty_uncovered_title to Res.string.swap_empty_uncovered_body
                        } else {
                            state.segment.emptyState()
                        }
                        EmptyState(
                            icon = Icons.Default.SwapHoriz,
                            title = stringResource(title),
                            body = stringResource(body),
                        )
                    } else {
                        SwapEventList(
                            rows = state.rows,
                            onTake = if (mine) null else { event -> pendingTake = event },
                        )
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

@Composable
private fun SwapEventList(rows: List<SwapRowUi>, onTake: ((DayEventUi) -> Unit)?) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val byMonth = remember(rows) { rows.groupBy { it.event.date.yearMonth } }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        byMonth.forEach { (month, monthRows) ->
            stickyHeader(key = month.toString(), contentType = "month") {
                MonthHeader(month)
            }
            items(monthRows, key = { it.event.id.value }, contentType = { "event" }) { row ->
                SwapEventRow(
                    event = row.event,
                    requestedBy = row.requestedBy,
                    coveredBy = row.coveredBy,
                    today = today,
                    onTake = if (onTake != null && row.event.canTake) {
                        { onTake(row.event) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth) {
    val months = stringArrayResource(Res.array.calendar_months)
    Text(
        text = stringResource(Res.string.calendar_month_year, months[month.month.ordinal], month.year),
        modifier = Modifier
            .fillMaxWidth()
            // Sticky, so it needs a ground of its own or the rows scroll through the letters.
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    ScreenHeader(
        title = stringResource(Res.string.swap_title),
        modifier = modifier.padding(vertical = 16.dp),
    )
}


@Composable
private fun UncoveredFilterChip(selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(Res.string.swap_filter_uncovered)) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
    )
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


private fun SwapSegment.emptyState(): Pair<StringResource, StringResource> = when (this) {
    SwapSegment.MINE ->
        Res.string.swap_empty_mine_title to Res.string.swap_empty_mine_body

    SwapSegment.COLLEAGUES ->
        Res.string.swap_empty_colleagues_title to Res.string.swap_empty_colleagues_body
}

private fun SwapMessage.resource(): StringResource = when (this) {
    SwapMessage.NotSwappable -> Res.string.event_swap_error_not_swappable
    SwapMessage.NotMember -> Res.string.event_swap_error_not_member
    SwapMessage.OwnShift -> Res.string.event_swap_error_own_shift
    SwapMessage.NotFound -> Res.string.event_swap_error_not_found
    SwapMessage.TakenBySomeoneElse -> Res.string.event_swap_error_taken_by_someone
    SwapMessage.SaveFailed -> Res.string.event_swap_error_save
}
