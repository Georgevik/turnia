package com.georgevik.turnia.ui.main.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.people.components.ColleagueCard
import com.georgevik.turnia.ui.main.system.EmptyState
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.people_empty_body
import turnia.app.shared.generated.resources.people_empty_title
import turnia.app.shared.generated.resources.people_load_error
import turnia.app.shared.generated.resources.people_search_hint

/**
 * "Personas" tab: whoever shared their calendar with this user. Tapping one opens their calendar.
 */
@Composable
fun PeopleScreen(viewModel: PeopleViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    val success = state as? PeopleUi.Success

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    success?.userMessage?.let { message ->
        val text = stringResource(Res.string.people_load_error)
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    val content = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))

    when (val current = state) {
        PeopleUi.Loading -> Box(content, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is PeopleUi.Success -> if (current.colleagues.isEmpty() && current.query.isBlank()) {
            EmptyState(
                icon = Icons.Default.PersonSearch,
                title = stringResource(Res.string.people_empty_title),
                body = stringResource(Res.string.people_empty_body),
                modifier = content,
            )
        } else {
            LazyColumn(
                modifier = content,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "search") {
                    OutlinedTextField(
                        value = current.query,
                        onValueChange = viewModel::searchBy,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(Res.string.people_search_hint)) },
                    )
                }

                items(current.colleagues, key = { it.id.value }) { colleague ->
                    ColleagueCard(
                        colleague = colleague,
                        onClick = {
                            navigator.goTo(
                                MainRoute.ExternalCalendar(
                                    ExternalCalendarData.Personal(colleague.id.value, colleague.name)
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}
