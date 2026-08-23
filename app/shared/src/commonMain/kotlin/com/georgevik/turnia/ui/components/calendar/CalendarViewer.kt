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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.components.calendar.animtransition.CALENDAR_TRANSITION_MILLIS
import com.georgevik.turnia.ui.components.calendar.daydetail.DayDetailsSheet
import com.georgevik.turnia.ui.components.calendar.daydetail.model.PredefinedEventUi
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
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

private const val WEEKS = 6

// The month pager can't be infinite, so it spans a large fixed range of months
// centered on an "anchor" page that maps to the current month. ~200 years each way
// is far more than anyone will scroll.
private const val MONTH_PAGE_COUNT = 12 * 400
private const val MONTH_PAGE_ANCHOR = MONTH_PAGE_COUNT / 2

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CalendarViewer(
    modifier: Modifier = Modifier,
    theme: CalendarTheme = CalendarThemes.myCalendar(),
    eventsByDate: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
    titleBar: @Composable () -> Unit = {},
    // Quick-add: predefined events offered when the user taps "+" on a day.
    predefinedEvents: List<PredefinedEventUi> = emptyList(),
    onAddPredefinedEvent: (date: LocalDate, predefinedId: String) -> Unit = { _, _ -> },
    onAddCustomEvent: (date: LocalDate) -> Unit = {},
    onManageEvent: (CalendarEventUi) -> Unit = {},
) {
    val anchorMonth = remember {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        LocalDate(today.year, today.month, 1)
    }
    val pagerState = rememberPagerState(initialPage = MONTH_PAGE_ANCHOR) { MONTH_PAGE_COUNT }
    val scope = rememberCoroutineScope()

    fun monthForPage(page: Int): LocalDate =
        anchorMonth.plus(page - MONTH_PAGE_ANCHOR, DateTimeUnit.MONTH)

    fun pageForMonth(target: LocalDate): Int =
        MONTH_PAGE_ANCHOR + anchorMonth.monthsUntil(target)

    // Whether the sheet is open — drives visibility; flips to false at once on close.
    var isSheetOpen by remember { mutableStateOf(false) }
    // The day the sheet renders and the tile shares bounds with. Retained through the
    // close animation so the morph keeps its content and a source to collapse back into.
    var sheetDate by remember { mutableStateOf<LocalDate?>(null) }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {

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
                    .padding(horizontal = 2.dp),
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
                )

                CalendarWeekTitles()

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val cellHeight = this.maxHeight / WEEKS
                    val eventArea = cellHeight - CalendarCellNumberHeight - 12.dp
                    val maxEventRows =
                        (eventArea / CalendarEventSlotHeight).toInt().coerceAtLeast(0)

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top,
                        // Keep neighbours ready so a swipe reveals a fully-laid-out month.
                        beyondViewportPageCount = 1,
                    ) { page ->
                        CalendarGrid(
                            month = monthForPage(page),
                            maxEventRows = maxEventRows,
                            calendarTheme = theme,
                            // "Selected" = the retained day, but only while open.
                            selectedDate = sheetDate.takeIf { isSheetOpen },
                            sharedDate = sheetDate,
                            eventsByDate = eventsByDate,
                            sharedScope = this@SharedTransitionLayout,
                            onDateSelected = { date ->
                                sheetDate = date
                                scope.launch {
                                    withFrameNanos { }
                                    isSheetOpen = true
                                }
                            },
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
                }
            }

            BottomSheetShadow(visible = isSheetOpen, onClick = { isSheetOpen = false })

            // BottomSheet
            AnimatedVisibility(
                visible = isSheetOpen,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                // Content uses the retained [sheetDate] so it stays stable while the
                // sheet animates back into the tile (isSheetOpen is already false).
                sheetDate?.let { date ->
                    DayDetailsSheet(
                        animatedVisibilityScope = this,
                        date = date,
                        events = eventsByDate[date].orEmpty(),
                        predefinedEvents = predefinedEvents,
                        onPickPredefined = { predefined ->
                            onAddPredefinedEvent(date, predefined.id)
                            isSheetOpen = false
                        },
                        onAddCustom = {
                            onAddCustomEvent(date)
                            isSheetOpen = false
                        },
                        onManageEvent = onManageEvent,
                    )
                }
            }
        }

    }
}

@Composable
private fun CalendarMonthHeader(
    pagerState: PagerState,
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
    maxEventRows: Int,
    calendarTheme: CalendarTheme,
    selectedDate: LocalDate?,
    sharedDate: LocalDate?,
    eventsByDate: Map<LocalDate, List<CalendarEventUi>>,
    sharedScope: SharedTransitionScope,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val leadingDays = month.dayOfWeek.ordinal
    val gridStart = month.minus(leadingDays, DateTimeUnit.DAY)

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
                        // Only the retained sheet date's in-month tile is a shared
                        // element (kept registered through the close animation).
                        sharedScope = if (dateInMonth && date == sharedDate) sharedScope else null,
                        isExpanded = date == selectedDate,
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
