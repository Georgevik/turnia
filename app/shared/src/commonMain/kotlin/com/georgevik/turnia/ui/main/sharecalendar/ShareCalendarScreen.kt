package com.georgevik.turnia.ui.main.sharecalendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.share_calendar_empty
import turnia.app.shared.generated.resources.share_calendar_load_error
import turnia.app.shared.generated.resources.share_calendar_title
import turnia.app.shared.generated.resources.share_calendar_unknown_user

/**
 * The people who can see this user's calendar. Granting and revoking are not wired yet.
 */
@Composable
fun ShareCalendarScreen(viewModel: ShareCalendarViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current

    uiState.userMessage?.let { message ->
        val text = message.message()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.share_calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = navigator::goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.calendar_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val content = Modifier.fillMaxSize().padding(innerPadding)

        when {
            uiState.loading -> Box(content, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.sharedWith.isEmpty() -> Box(
                modifier = content.padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.share_calendar_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                modifier = content,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.sharedWith, key = { it.id }) { user ->
                    TListItem(
                        title = user.name.ifBlank {
                            stringResource(Res.string.share_calendar_unknown_user)
                        },
                        subtitle = user.username.takeIf { it.isNotBlank() }?.let { "@$it" },
                        leading = {
                            Avatar(
                                background = entityColor(user.id),
                                icon = Icons.Default.Person,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareCalendarMessage.message(): String = stringResource(
    when (this) {
        ShareCalendarMessage.LoadFailed -> Res.string.share_calendar_load_error
    }
)
