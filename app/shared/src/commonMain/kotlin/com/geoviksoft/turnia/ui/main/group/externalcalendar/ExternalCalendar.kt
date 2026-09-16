package com.geoviksoft.turnia.ui.main.group.externalcalendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.LocalRootNavigator
import com.geoviksoft.turnia.navigation.main.routes.ExternalCalendarData
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.navigation.routes.EventTypeDetailData
import com.geoviksoft.turnia.ui.components.calendar.CalendarThemes
import com.geoviksoft.turnia.ui.components.calendar.CalendarViewer
import com.geoviksoft.turnia.ui.components.calendar.components.CalendarTitleBar
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUiPreview.A_DAY_EVENT
import com.geoviksoft.turnia.ui.components.daydetail.DayAddMode
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.shared_calendar_error_load
import turnia.app.shared.generated.resources.shared_calendar_error_not_shared
import turnia.app.shared.generated.resources.shared_calendar_error_range

/**
 * A calendar that is not the user's own: a group's, or a colleague's. Read and add only — leaving
 * and deleting the group belong to its detail, behind the info button.
 */
@Composable
fun ExternalCalendar(viewModel: ExternalCalendarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExternalCalendarContent(
        uiState,
        viewModel.data,
        viewModel::userMessageShown,
        viewModel::onMonthChanged
    )
}

@Composable
private fun ExternalCalendarContent(
    uiState: GroupCalendarUi,
    data: ExternalCalendarData,
    onSnackbarShown: () -> Unit,
    onMonthChanged: (LocalDate) -> Unit
) {
    val navigator = LocalNavigator.current
    val rootNavigator = LocalRootNavigator.current
    val snackbar = LocalSnackbar.current

    val isGroup = data is ExternalCalendarData.Group
    val theme = if (isGroup) CalendarThemes.group() else CalendarThemes.colleague()

    val addMode = addModeOf(data, isRevoked = uiState.isRevoked)

    uiState.userMessage?.let { message ->
        val text = message.message()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            onSnackbarShown()
        }
    }

    CalendarViewer(
        theme = theme,
        addMode = addMode,
        titleBar = {
            CalendarTitleBar(
                title = data.name,
                icon = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                theme = theme,
                onBack = navigator::goBack,
                onInfo = if (data is ExternalCalendarData.Group && !uiState.isRevoked) {
                    { navigator.goTo(MainRoute.GroupDetail(data.id)) }
                } else {
                    null
                },
            )
        },
        onMonthChanged = onMonthChanged,
        // Without this a group with no types has no way out of an empty add pane at all.
        onEditGroup = { groupId, _ -> navigator.goTo(MainRoute.GroupDetail(groupId)) },
        onAddGroupType = { groupId ->
            rootNavigator.goTo(RootRoute.EventTypeDetailKey(EventTypeDetailData.NewGroup(groupId)))
        },
        eventsByDate = uiState.events,
        isLoading = uiState.loading,
    )
}

private fun addModeOf(data: ExternalCalendarData, isRevoked: Boolean): DayAddMode = when {
    data !is ExternalCalendarData.Group -> DayAddMode.Disabled
    isRevoked -> DayAddMode.Disabled
    else -> DayAddMode.GroupOnly(GroupId(data.id))
}

@Composable
private fun SharedCalendarError.message(): String = stringResource(
    when (this) {
        SharedCalendarError.NotShared -> Res.string.shared_calendar_error_not_shared
        SharedCalendarError.RangeTooWide -> Res.string.shared_calendar_error_range
        SharedCalendarError.LoadFailed -> Res.string.shared_calendar_error_load
    }
)

@Preview
@Composable
fun ExternalCalendarGroupPreview() {
    PreviewTurniaTheme {
        ExternalCalendarContent(
            uiState = GroupCalendarUi(
                events = mapOf(LocalDate(2026, 2, 20) to listOf(A_DAY_EVENT)),
                loading = true,
                isRevoked = false,
                userMessage = null,
            ),
            data = ExternalCalendarData.Group("id", "Urgencias"),
            onSnackbarShown = {},
            onMonthChanged = {},
        )
    }
}

@Preview
@Composable
fun ExternalCalendarPersonalPreview() {
    PreviewTurniaTheme {
        ExternalCalendarContent(
            uiState = GroupCalendarUi(
                events = mapOf(LocalDate(2026, 2, 20) to listOf(A_DAY_EVENT)),
                loading = false,
                isRevoked = false,
                userMessage = null,
            ),
            data = ExternalCalendarData.Personal("id", "Antonio Rodriguez"),
            onSnackbarShown = {},
            onMonthChanged = {},
        )
    }
}
