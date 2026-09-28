package com.geoviksoft.turnia.ui.components.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.ui.components.shareprompt.SharePromptHost
import com.geoviksoft.turnia.ui.components.shareprompt.SharePromptViewModel
import com.geoviksoft.turnia.ui.components.teamprompt.TeamPromptSheet
import com.geoviksoft.turnia.ui.components.teamprompt.TeamPromptViewModel
import org.koin.compose.viewmodel.koinViewModel

/** The calendar prompts, one of which a visit of the calendar may show. */
enum class CalendarPrompt { Team, Share }

/**
 * The one place that decides which calendar prompt is on screen, so two can never stack. When both
 * are due, the team prompt wins. Whichever one a visit shows first keeps the screen: the other
 * waits for the next time a calendar opens, even when it only became due while the first was up —
 * the team prompt waits on a server read, so it can arrive after the share prompt is on screen.
 *
 * [shownThisVisit] belongs to the visit of the calendar, not to this host, which leaves the
 * composition every time a day sheet opens.
 */
@Composable
fun CalendarPromptHost(
    allowSharePrompt: Boolean,
    allowTeamPrompt: Boolean,
    shownThisVisit: CalendarPrompt?,
    onShown: (CalendarPrompt) -> Unit,
    teamPromptViewModel: TeamPromptViewModel = koinViewModel(),
    sharePromptViewModel: SharePromptViewModel = koinViewModel(),
) {
    val teamPending by teamPromptViewModel.pending.collectAsStateWithLifecycle()
    val sharePending by sharePromptViewModel.pending.collectAsStateWithLifecycle()

    val showTeam = allowTeamPrompt && teamPending && shownThisVisit != CalendarPrompt.Share
    val showShare = allowSharePrompt && sharePending != null && shownThisVisit != CalendarPrompt.Team

    when {
        showTeam -> {
            LaunchedEffect(Unit) { onShown(CalendarPrompt.Team) }
            TeamPromptSheet(teamPromptViewModel)
        }

        showShare -> {
            LaunchedEffect(Unit) { onShown(CalendarPrompt.Share) }
            SharePromptHost(sharePromptViewModel)
        }
    }
}
