package com.geoviksoft.turnia.ui.components.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.ui.components.shareprompt.SharePromptHost
import com.geoviksoft.turnia.ui.components.teamprompt.TeamPromptSheet
import com.geoviksoft.turnia.ui.components.teamprompt.TeamPromptViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The one place that decides which calendar prompt is on screen, so two can never stack. The team
 * prompt wins; a share prompt due at the same time keeps its milestone and waits for the next time
 * a calendar opens — [teamPromptShown] belongs to this visit of the calendar, not to this host,
 * which leaves the composition every time a day sheet opens.
 */
@Composable
fun CalendarPromptHost(
    allowSharePrompt: Boolean,
    allowTeamPrompt: Boolean,
    teamPromptShown: Boolean,
    onTeamPromptShown: () -> Unit,
    teamPromptViewModel: TeamPromptViewModel = koinViewModel(),
) {
    val teamPending by teamPromptViewModel.pending.collectAsStateWithLifecycle()

    if (allowTeamPrompt && teamPending) {
        LaunchedEffect(Unit) { onTeamPromptShown() }
        TeamPromptSheet(teamPromptViewModel)
        return
    }

    if (allowSharePrompt && !teamPromptShown) SharePromptHost()
}
