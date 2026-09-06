package com.georgevik.turnia.ui.main.group.externalcalendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.LocalRootNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.components.calendar.CalendarThemes
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import com.georgevik.turnia.ui.components.calendar.components.CalendarTitleBar
import com.georgevik.turnia.ui.components.calendar.model.ThreeDotsOption
import com.georgevik.turnia.ui.components.daydetail.DayAddMode
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.components.ConfirmationDialog
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.group_leave_action
import turnia.app.shared.generated.resources.group_leave_confirm
import turnia.app.shared.generated.resources.group_leave_error
import turnia.app.shared.generated.resources.group_leave_last_admin_error
import turnia.app.shared.generated.resources.group_leave_message
import turnia.app.shared.generated.resources.group_leave_title

@Composable
fun ExternalCalendar(viewModel: ExternalCalendarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    // Group detail covers the whole screen, so it goes on the root stack, not this tab's.
    val rootNavigator = LocalRootNavigator.current
    val snackbar = LocalSnackbar.current
    val data = viewModel.data
    val isGroup = data is ExternalCalendarData.Group
    val theme = if (isGroup) CalendarThemes.group() else CalendarThemes.colleague()

    var leaveRequested by remember { mutableStateOf(false) }

    // Nothing left to show once they are out: the group's calendar is no longer theirs to open,
    // and their own leftover shifts are on their calendar.
    LaunchedEffect(uiState.hasLeft) {
        if (uiState.hasLeft) navigator.goBack()
    }

    uiState.userMessage?.let { message ->
        val text = stringResource(message.text())
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    // Someone removed from the group keeps their leftover shifts on screen but adds nothing more,
    // and the group's detail is closed to them along with the group document it reads.
    val addMode = when {
        data !is ExternalCalendarData.Group -> DayAddMode.Disabled
        uiState.isRevoked -> DayAddMode.Disabled
        else -> DayAddMode.GroupOnly(GroupId(data.id))
    }

    val contextualOptions = if (isGroup && !uiState.isRevoked) {
        listOf(
            ThreeDotsOption(
                text = stringResource(Res.string.group_leave_action),
                leadingIcon = Icons.AutoMirrored.Filled.Logout,
                onClick = { leaveRequested = true },
            )
        )
    } else {
        emptyList()
    }

    CalendarViewer(
        theme = theme,
        addMode = addMode,
        contextualOptions = contextualOptions,
        titleBar = {
            CalendarTitleBar(
                title = data.name,
                icon = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                theme = theme,
                onBack = navigator::goBack,
                onInfo = if (data is ExternalCalendarData.Group && !uiState.isRevoked) {
                    { rootNavigator.goTo(RootRoute.GroupDetailKey(data.id)) }
                } else {
                    null
                },
            )
        },
        onMonthChanged = viewModel::onMonthChanged,
        eventsByDate = uiState.events,
    )

    if (leaveRequested) {
        ConfirmationDialog(
            title = stringResource(Res.string.group_leave_title),
            message = stringResource(Res.string.group_leave_message),
            confirmText = stringResource(Res.string.group_leave_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                leaveRequested = false
                viewModel.onLeaveGroup()
            },
            onDismissRequest = { leaveRequested = false },
        )
    }
}

private fun GroupCalendarMessage.text(): StringResource = when (this) {
    GroupCalendarMessage.LeaveFailed -> Res.string.group_leave_error
    GroupCalendarMessage.LeaveLastAdmin -> Res.string.group_leave_last_admin_error
}
