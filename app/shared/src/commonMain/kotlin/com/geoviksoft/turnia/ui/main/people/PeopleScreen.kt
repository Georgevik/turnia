package com.geoviksoft.turnia.ui.main.people

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.main.routes.ExternalCalendarData
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.people.components.PeopleFilterChips
import com.geoviksoft.turnia.ui.main.people.components.PersonCard
import com.geoviksoft.turnia.ui.main.people.components.ShareCalendarSheet
import com.geoviksoft.turnia.ui.main.people.components.SwipeSide
import com.geoviksoft.turnia.ui.main.people.components.SwipeablePersonRow
import com.geoviksoft.turnia.ui.main.people.components.displayName
import com.geoviksoft.turnia.ui.main.people.model.PeopleFilter
import com.geoviksoft.turnia.ui.main.people.model.PersonRowUi
import com.geoviksoft.turnia.ui.main.system.EmptyState
import com.geoviksoft.turnia.ui.main.system.ScreenHeader
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.TurniaSnackbarVisual
import com.geoviksoft.turnia.ui.system.ads.AdBanner
import com.geoviksoft.turnia.ui.system.components.Chevron
import com.geoviksoft.turnia.ui.system.components.ConfirmationDialog
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.people_all_hidden_body
import turnia.app.shared.generated.resources.people_all_hidden_title
import turnia.app.shared.generated.resources.people_empty_body
import turnia.app.shared.generated.resources.people_empty_title
import turnia.app.shared.generated.resources.people_hidden_message
import turnia.app.shared.generated.resources.people_hide
import turnia.app.shared.generated.resources.people_hide_error
import turnia.app.shared.generated.resources.people_load_error
import turnia.app.shared.generated.resources.people_shared_empty_title
import turnia.app.shared.generated.resources.people_title
import turnia.app.shared.generated.resources.people_undo
import turnia.app.shared.generated.resources.people_unhidden_message
import turnia.app.shared.generated.resources.people_unhide
import turnia.app.shared.generated.resources.people_unhide_error
import turnia.app.shared.generated.resources.share_calendar_add
import turnia.app.shared.generated.resources.share_calendar_empty
import turnia.app.shared.generated.resources.share_calendar_grant_error
import turnia.app.shared.generated.resources.share_calendar_load_error
import turnia.app.shared.generated.resources.share_calendar_revoke
import turnia.app.shared.generated.resources.share_calendar_revoke_confirm
import turnia.app.shared.generated.resources.share_calendar_revoke_error
import turnia.app.shared.generated.resources.share_calendar_revoke_message
import turnia.app.shared.generated.resources.share_calendar_revoke_title

/**
 * "Personas" tab: both directions of a calendar grant, one chip each — who this user shares their
 * calendar with, and whoever shares theirs back. Tapping one of the latter opens their calendar;
 * swiping it hides it under a third chip, without touching the grant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(viewModel: PeopleViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val success = state as? PeopleUi.Success

    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var pendingRevoke by remember { mutableStateOf<PersonRowUi?>(null) }
    val sheetState = rememberModalBottomSheetState()

    success?.userMessage?.let { message ->
        val text = message.text()
        val undo = stringResource(Res.string.people_undo)
        LaunchedEffect(message) {
            if (!message.offersUndo()) {
                snackbar.showSnackbar(text.toErrorSnackbar())
                viewModel.userMessageShown(message)
                return@LaunchedEffect
            }

            try {
                val result = snackbar.showSnackbar(
                    TurniaSnackbarVisual(text, actionLabel = undo, duration = SnackbarDuration.Long)
                )
                viewModel.userMessageShown(message, undo = result == SnackbarResult.ActionPerformed)
            } finally {
                // Leaving the tab cancels the snackbar before it is answered. An Undo offered again
                // on the way back, for a hide long done, would only surprise.
                viewModel.userMessageShown(message)
            }
        }
    }

    Scaffold(
        bottomBar = { AdBanner() },
        floatingActionButton = {
            if (success?.filter == PeopleFilter.SHARED_BY_ME && success.sharedByMe.isNotEmpty()) {
                FloatingActionButton(onClick = { sheetOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = stringResource(Res.string.share_calendar_add),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ScreenHeader(
                title = stringResource(Res.string.people_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )

            when (val current = state) {
                PeopleUi.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                is PeopleUi.Success -> {
                    PeopleFilterChips(
                        selected = current.filter,
                        hiddenCount = current.hidden.size,
                        onSelected = viewModel::filterSelected,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    when (current.filter) {
                        PeopleFilter.SHARED_BY_ME -> SharedByMe(
                            people = current.sharedByMe,
                            onShare = { sheetOpen = true },
                            onRevoke = { pendingRevoke = it },
                        )

                        PeopleFilter.SHARED_WITH_ME -> SharedWithMe(
                            people = current.sharedWithMe,
                            anyHidden = current.hidden.isNotEmpty(),
                            onHide = viewModel::onHide,
                            swipeReset = current.userMessage.failedSwipe(),
                        )

                        PeopleFilter.HIDDEN -> Hidden(
                            people = current.hidden,
                            onUnhide = viewModel::onUnhide,
                            swipeReset = current.userMessage.failedSwipe(),
                        )
                    }
                }
            }
        }
    }

    if (sheetOpen && success != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetOpen = false
                viewModel.onSearchDismissed()
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                ShareCalendarSheet(
                    search = success.search,
                    onQueryChanged = viewModel::onSearchChanged,
                    onPick = { userId ->
                        sheetOpen = false
                        viewModel.onGrant(userId)
                    },
                )
            }
        }
    }

    pendingRevoke?.let { person ->
        ConfirmationDialog(
            title = stringResource(Res.string.share_calendar_revoke_title),
            message = stringResource(
                Res.string.share_calendar_revoke_message,
                person.displayName()
            ),
            confirmText = stringResource(Res.string.share_calendar_revoke_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                pendingRevoke = null
                viewModel.onRevoke(person.id)
            },
            onDismissRequest = { pendingRevoke = null },
        )
    }
}

@Composable
private fun SharedByMe(
    people: List<PersonRowUi>,
    onShare: () -> Unit,
    onRevoke: (PersonRowUi) -> Unit,
) {
    if (people.isEmpty()) {
        EmptyState(
            icon = Icons.Default.PersonAdd,
            title = stringResource(Res.string.people_shared_empty_title),
            body = stringResource(Res.string.share_calendar_empty),
            action = stringResource(Res.string.share_calendar_add),
            onAction = onShare,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    PeopleList(people) { person ->
        PersonCard(
            person = person,
            modifier = Modifier.testTag(TestTags.personRow(person.id)),
            trailing = {
                IconButton(onClick = { onRevoke(person) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.share_calendar_revoke),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
    }
}

@Composable
private fun SharedWithMe(
    people: List<PersonRowUi>,
    anyHidden: Boolean,
    onHide: (UserId) -> Unit,
    swipeReset: PeopleMessage?,
) {
    if (people.isEmpty() && anyHidden) {
        EmptyState(
            icon = Icons.Default.VisibilityOff,
            title = stringResource(Res.string.people_all_hidden_title),
            body = stringResource(Res.string.people_all_hidden_body),
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    if (people.isEmpty()) {
        EmptyState(
            icon = Icons.Default.PersonSearch,
            title = stringResource(Res.string.people_empty_title),
            body = stringResource(Res.string.people_empty_body),
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val navigator = LocalNavigator.current

    val hide = stringResource(Res.string.people_hide)

    PeopleList(people) { person ->
        SwipeablePersonRow(
            person = person,
            side = SwipeSide.END_TO_START,
            actionLabel = hide,
            actionIcon = Icons.Default.VisibilityOff,
            onAction = { onHide(person.id) },
            onClick = {
                navigator.goTo(
                    MainRoute.ExternalCalendar(
                        ExternalCalendarData.Personal(person.id.value, person.name)
                    )
                )
            },
            trailing = { Chevron() },
            resetKey = swipeReset,
        )
    }
}

/** Calendars this user has hidden: they are not opened from here, only brought back. */
@Composable
private fun Hidden(
    people: List<PersonRowUi>,
    onUnhide: (UserId) -> Unit,
    swipeReset: PeopleMessage?,
) {
    val show = stringResource(Res.string.people_unhide)

    PeopleList(people) { person ->
        SwipeablePersonRow(
            person = person,
            side = SwipeSide.START_TO_END,
            actionLabel = show,
            actionIcon = Icons.Default.Visibility,
            onAction = { onUnhide(person.id) },
            trailing = {
                IconButton(onClick = { onUnhide(person.id) }) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = show)
                }
            },
            resetKey = swipeReset,
        )
    }
}

private fun PeopleMessage.offersUndo(): Boolean =
    this is PeopleMessage.Hidden || this is PeopleMessage.Unhidden

/** A failed hide or show leaves its row in the list, so the swipe that started it has to snap back. */
private fun PeopleMessage?.failedSwipe(): PeopleMessage? =
    takeIf { it is PeopleMessage.HideFailed || it is PeopleMessage.UnhideFailed }

@Composable
private fun PeopleList(people: List<PersonRowUi>, row: @Composable (PersonRowUi) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(people, key = { it.id.value }) { person -> row(person) }
    }
}

@Composable
private fun PeopleMessage.text(): String = stringResource(
    when (this) {
        PeopleMessage.SharedWithMeLoadFailed -> Res.string.people_load_error
        PeopleMessage.SharedByMeLoadFailed -> Res.string.share_calendar_load_error
        PeopleMessage.GrantFailed -> Res.string.share_calendar_grant_error
        PeopleMessage.RevokeFailed -> Res.string.share_calendar_revoke_error
        PeopleMessage.HideFailed -> Res.string.people_hide_error
        PeopleMessage.UnhideFailed -> Res.string.people_unhide_error
        is PeopleMessage.Hidden -> Res.string.people_hidden_message
        is PeopleMessage.Unhidden -> Res.string.people_unhidden_message
    }
)
