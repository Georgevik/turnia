package com.geoviksoft.turnia.ui.components.shareprompt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.ShareLink
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.SharePromptAnswer
import com.geoviksoft.turnia.core.domain.model.SharePromptAudience
import com.geoviksoft.turnia.ui.system.LocalTextSharer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.share_prompt_coworkers_message
import turnia.app.shared.generated.resources.share_prompt_coworkers_text
import turnia.app.shared.generated.resources.share_prompt_coworkers_title
import turnia.app.shared.generated.resources.share_prompt_friends_message
import turnia.app.shared.generated.resources.share_prompt_friends_text
import turnia.app.shared.generated.resources.share_prompt_friends_title
import turnia.app.shared.generated.resources.share_prompt_not_now
import turnia.app.shared.generated.resources.share_prompt_share

/**
 * The prompt waiting to be shown, if any. The caller hosts it only while its own sheets are closed,
 * so it never lands on top of the day that triggered it.
 */
@Composable
fun SharePromptHost(viewModel: SharePromptViewModel = koinViewModel()) {
    val prompt by viewModel.pending.collectAsStateWithLifecycle()
    prompt?.let { SharePromptSheet(it, viewModel) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharePromptSheet(prompt: SharePrompt, viewModel: SharePromptViewModel) {
    val copy = prompt.audience.copy()
    val sharer = LocalTextSharer.current
    val message = stringResource(copy.message)

    LaunchedEffect(prompt) { viewModel.shown(prompt) }

    ModalBottomSheet(onDismissRequest = { viewModel.answered(prompt, SharePromptAnswer.Dismissed) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(copy.title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(copy.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    sharer.share("$message ${ShareLink.of(prompt.audience)}")
                    viewModel.answered(prompt, SharePromptAnswer.Shared)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.share_prompt_share))
            }
            TextButton(
                onClick = { viewModel.answered(prompt, SharePromptAnswer.Dismissed) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.share_prompt_not_now))
            }
        }
    }
}

private class SharePromptCopy(
    val title: StringResource,
    val text: StringResource,
    val message: StringResource,
)

private fun SharePromptAudience.copy() = when (this) {
    SharePromptAudience.Coworkers -> SharePromptCopy(
        Res.string.share_prompt_coworkers_title,
        Res.string.share_prompt_coworkers_text,
        Res.string.share_prompt_coworkers_message,
    )

    SharePromptAudience.Friends -> SharePromptCopy(
        Res.string.share_prompt_friends_title,
        Res.string.share_prompt_friends_text,
        Res.string.share_prompt_friends_message,
    )
}
