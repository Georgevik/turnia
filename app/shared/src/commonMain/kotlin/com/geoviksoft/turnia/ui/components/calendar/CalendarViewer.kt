package com.geoviksoft.turnia.ui.components.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.ui.components.calendar.model.CalendarCellEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.ThreeDotsOption
import com.geoviksoft.turnia.ui.components.daydetail.DayAddMode
import com.geoviksoft.turnia.ui.components.daydetail.DayDetailSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.monthsUntil
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
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val WEEKS = 6

private val LOADING_DELAY = 300.milliseconds

// The month pager can't be infinite, so it spans a large fixed range of months
// centered on an "anchor" page that maps to the current month. ~200 years each way
// is far more than anyone will scroll.
private const val MONTH_PAGE_COUNT = 12 * 400
private const val MONTH_PAGE_ANCHOR = MONTH_PAGE_COUNT / 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarViewer(
    modifier: Modifier = Modifier,
    theme: CalendarTheme = CalendarThemes.myCalendar(),
    eventsByDate: Map<LocalDate, List<DayEventUi>> = emptyMap(),
    isLoading: Boolean = false,
    addMode: DayAddMode,
    titleBar: @Composable () -> Unit = {},
    contextualOptions: List<ThreeDotsOption> = emptyList(),
    onMonthChanged: (LocalDate) -> Unit,
    onEditGroup: (groupId: String, groupName: String) -> Unit = { _, _ -> },
    onAddPersonalType: () -> Unit = {},
    onAddGroupType: (groupId: String) -> Unit = {},
) {
    val anchorMonth = remember {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        LocalDate(today.year, today.month, 1)
    }
    val pagerState = rememberPagerState(initialPage = MONTH_PAGE_ANCHOR) { MONTH_PAGE_COUNT }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                val newMonth = anchorMonth.plus(page - MONTH_PAGE_ANCHOR, DateTimeUnit.MONTH)
                Logger.d("Jorge", "Month changed to $newMonth")
                onMonthChanged(newMonth)
            }
    }


    val scope = rememberCoroutineScope()

    val cellsByDate = remember(eventsByDate) {
        eventsByDate.mapValues { (_, events) -> events.map { it.cell } }
    }
    fun monthForPage(page: Int): LocalDate =
        anchorMonth.plus(page - MONTH_PAGE_ANCHOR, DateTimeUnit.MONTH)

    fun pageForMonth(target: LocalDate): Int =
        MONTH_PAGE_ANCHOR + anchorMonth.monthsUntil(target)

    // The day whose details sheet is shown; non-null means the sheet is open.
    var sheetDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Animate the sheet out, then clear the date. Used by the programmatic close
    // paths (add event, edit group) that don't go through onDismissRequest.
    fun dismissSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) sheetDate = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Keep the top (status bar) inset, but only a small horizontal margin.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(all = 2.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                titleBar()
                CalendarMonthHeader(
                    pagerState = pagerState,
                    monthForPage = ::monthForPage,
                    onPrevious = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    onNext = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    contextualOptions = contextualOptions
                )

                CalendarWeekTitles()

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top,
                        // Keep neighbours ready so a swipe reveals a fully-laid-out month.
                        beyondViewportPageCount = 1,
                    ) { page ->
                        CalendarGrid(
                            month = monthForPage(page),
                            calendarTheme = theme,
                            // Highlight the open day's tile while its sheet is up.
                            selectedDate = sheetDate,
                            cellsByDate = cellsByDate,
                            onDateSelected = { date -> sheetDate = date },
                            onMonthChanged = { newMonth ->
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pageForMonth(
                                            newMonth
                                        )
                                    )
                                }
                            },
                        )
                    }
                    CalendarLoading(
                        isLoading = isLoading,
                        theme = theme,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

        }

        sheetDate?.let { date ->
            ModalBottomSheet(
                onDismissRequest = { sheetDate = null },
                sheetState = sheetState,
            ) {
                DayDetailSheet(
                    date = date,
                    events = eventsByDate[date].orEmpty(),
                    addMode = addMode,
                    openEditTypeScreen = { groupId, groupName ->
                        onEditGroup(groupId, groupName)
                        dismissSheet()
                    },
                    openNewPersonalTypeScreen = { onAddPersonalType() },
                    openNewGroupTypeScreen = onAddGroupType,
                    onClose = { dismissSheet() },
                )
            }
        }
    }
}

/**
 * Laid over the grid rather than in its place: the cache has usually painted most of the month
 * already, and the days stay tappable underneath. It waits [LOADING_DELAY] before appearing, so a
 * read that settles straight away — the usual case — never flashes it.
 */
@Composable
private fun CalendarLoading(
    isLoading: Boolean,
    theme: CalendarTheme,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (isLoading) delay(LOADING_DELAY)
        visible = isLoading
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = theme.accentColor)
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    pagerState: PagerState,
    contextualOptions: List<ThreeDotsOption>,
    monthForPage: (Int) -> LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            contentAlignment = Alignment.CenterStart,
        ) {
            val monthNames = stringArrayResource(Res.array.calendar_months)
            val currentPage = pagerState.currentPage

            // Only the current page and its two neighbours can be on screen at once.
            (currentPage - 1..currentPage + 1).forEach { page ->
                val month = monthForPage(page)
                Text(
                    text = stringResource(
                        Res.string.calendar_month_year,
                        monthNames[month.month.ordinal],
                        month.year,
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Distance (in pages) from the settled viewport position.
                            val progress =
                                page - (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            translationX = progress * size.width
                            alpha = (1f - abs(progress)).coerceIn(0f, 1f)
                        },
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Row(modifier = Modifier.wrapContentSize()) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(Res.string.calendar_previous_month),
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(Res.string.calendar_next_month),
                )
            }

            if (contextualOptions.isNotEmpty()) {
                ThreeDotsContextMenu(contextualOptions)
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
private fun CalendarGrid(
    month: LocalDate,
    calendarTheme: CalendarTheme,
    selectedDate: LocalDate?,
    cellsByDate: Map<LocalDate, List<CalendarCellEventUi>>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val leadingDays = month.dayOfWeek.ordinal
    val gridStart = month.minus(leadingDays, DateTimeUnit.DAY)
    val stagger = remember { EventEntranceStagger() }

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
                        events = cellsByDate[date].orEmpty(),
                        stagger = stagger,
                        onClick = {
                            if (!dateInMonth) {
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
