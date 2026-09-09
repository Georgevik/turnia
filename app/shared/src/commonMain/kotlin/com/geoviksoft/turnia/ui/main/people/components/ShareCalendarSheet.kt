package com.geoviksoft.turnia.ui.main.people.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.ui.main.people.SearchResultUi
import com.geoviksoft.turnia.ui.main.people.SearchUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.TListItem
import com.geoviksoft.turnia.ui.system.keyboardAware
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.share_calendar_already_shared
import turnia.app.shared.generated.resources.share_calendar_search_empty
import turnia.app.shared.generated.resources.share_calendar_search_hint
import turnia.app.shared.generated.resources.share_calendar_search_min
import turnia.app.shared.generated.resources.share_calendar_unknown_user

/** Username search: who this user can hand read access to their calendar. */
@Composable
fun ShareCalendarSheet(
    search: SearchUi,
    onQueryChanged: (String) -> Unit,
    onPick: (UserId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .keyboardAware()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = search.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            prefix = { Text("@") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(stringResource(Res.string.share_calendar_search_hint)) },
        )

        when (val panel = search.panel) {
            SearchUi.Panel.Searching -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            SearchUi.Panel.TooShort ->
                Message(stringResource(Res.string.share_calendar_search_min))

            SearchUi.Panel.Empty ->
                Message(stringResource(Res.string.share_calendar_search_empty))

            is SearchUi.Panel.Results -> {
                BoxWithConstraints(modifier = Modifier.clickable(enabled = false, onClick = {})) {
                    SearchUserList(panel.users, onPick)
                    if (panel.isLoading) {
                        Box(
                            modifier = Modifier.matchParentSize()
                                .background(Color.White.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserList(users: List<SearchResultUi>, onPick: (UserId) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users, key = { it.id.value }) { result ->
            TListItem(
                title = result.name.ifBlank {
                    stringResource(Res.string.share_calendar_unknown_user)
                },
                subtitle = "@${result.username}",
                onClick = if (result.alreadyShared) null else ({ onPick(result.id) }),
                leading = { PersonAvatar(result.id) },
                trailing = if (result.alreadyShared) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(Res.string.share_calendar_already_shared),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun Message(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
)

@Preview
@Composable
fun PreviewShareCalendarSheet() {
    val searchPanelResult = SearchUi.Panel.Results(
        users = listOf(
            SearchResultUi(
                id = UserId("1"),
                name = "George Vik",
                username = "georgevik",
                alreadyShared = false,
            ),
            SearchResultUi(
                id = UserId("2"),
                name = "Second",
                username = "thesecond",
                alreadyShared = false,
            )
        ), isLoading = true
    )

    val searchPanelEmpty = SearchUi.Panel.Empty
    val searchPanelTooShort = SearchUi.Panel.TooShort
    val searchPanelSearching = SearchUi.Panel.Searching

    PreviewTurniaTheme {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ShareCalendarSheet(
                search = SearchUi(
                    query = "Query",
                    panel = searchPanelResult
                ),
                onQueryChanged = {},
                onPick = {},
                modifier = Modifier.background(Color.White)
            )

            ShareCalendarSheet(
                search = SearchUi(
                    query = "Query",
                    panel = searchPanelEmpty
                ),
                onQueryChanged = {},
                onPick = {},
                modifier = Modifier.background(Color.White)
            )
            ShareCalendarSheet(
                search = SearchUi(
                    query = "Query",
                    panel = searchPanelTooShort
                ),
                onQueryChanged = {},
                onPick = {},
                modifier = Modifier.background(Color.White)
            )
            ShareCalendarSheet(
                search = SearchUi(
                    query = "Query",
                    panel = searchPanelSearching
                ),
                onQueryChanged = {},
                onPick = {},
                modifier = Modifier.background(Color.White)
            )
        }


    }
}
