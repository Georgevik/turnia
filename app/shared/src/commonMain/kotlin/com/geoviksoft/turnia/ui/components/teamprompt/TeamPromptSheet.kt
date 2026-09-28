package com.geoviksoft.turnia.ui.components.teamprompt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.team_prompt_create
import turnia.app.shared.generated.resources.team_prompt_join
import turnia.app.shared.generated.resources.team_prompt_not_now
import turnia.app.shared.generated.resources.team_prompt_text
import turnia.app.shared.generated.resources.team_prompt_title

/** "Do you work with a team?", shown once per device while the prompt is pending. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPromptSheet(viewModel: TeamPromptViewModel) {
    val navigator = LocalNavigator.current

    LaunchedEffect(Unit) { viewModel.shown() }

    ModalBottomSheet(onDismissRequest = { viewModel.answered(TeamPromptChoice.Dismissed) }) {
        TeamPromptContent(
            onCreate = {
                if (viewModel.answered(TeamPromptChoice.Create)) {
                    navigator.goTo(MainRoute.GroupDetail(groupId = null))
                }
            },
            onJoin = { viewModel.answered(TeamPromptChoice.Join) },
            onNotNow = { viewModel.answered(TeamPromptChoice.Dismissed) },
        )
    }
}

@Composable
private fun TeamPromptContent(
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.team_prompt_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(Res.string.team_prompt_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.team_prompt_create))
        }
        OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.team_prompt_join))
        }
        TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.team_prompt_not_now))
        }
    }
}

@Preview
@Composable
private fun TeamPromptContentPreview() {
    PreviewTurniaTheme {
        TeamPromptContent(onCreate = {}, onJoin = {}, onNotNow = {})
    }
}
