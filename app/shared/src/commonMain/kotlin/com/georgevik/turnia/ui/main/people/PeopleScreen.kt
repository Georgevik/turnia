package com.georgevik.turnia.ui.main.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.people.components.ColleagueCard
import com.georgevik.turnia.ui.main.people.model.ColleagueRowUi
import com.georgevik.turnia.ui.main.system.EmptyState
import com.georgevik.turnia.ui.main.system.ScreenHeader
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.people_empty_body
import turnia.app.shared.generated.resources.people_empty_title
import turnia.app.shared.generated.resources.people_load_error
import turnia.app.shared.generated.resources.people_title

/**
 * "Personas" tab: whoever shared their calendar with this user. Tapping one opens their calendar.
 */
@Composable
fun PeopleScreen(viewModel: PeopleViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val success = state as? PeopleUi.Success

    success?.userMessage?.let { message ->
        val text = stringResource(Res.string.people_load_error)
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        ScreenHeader(
            title = stringResource(Res.string.people_title),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )

        when (val current = state) {
            PeopleUi.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is PeopleUi.Success -> if (current.colleagues.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = stringResource(Res.string.people_empty_title),
                    body = stringResource(Res.string.people_empty_body),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PeopleList(modifier = Modifier.fillMaxSize(), colleagues = current.colleagues)
            }
        }
    }
}

@Composable
private fun PeopleList(modifier: Modifier = Modifier, colleagues: List<ColleagueRowUi>) {
    val navigator = LocalNavigator.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(colleagues, key = { it.id.value }) { colleague ->
            ColleagueCard(
                colleague = colleague,
                onClick = {
                    navigator.goTo(
                        MainRoute.ExternalCalendar(
                            ExternalCalendarData.Personal(
                                colleague.id.value,
                                colleague.name,
                            )
                        )
                    )
                },
            )
        }
    }
}
