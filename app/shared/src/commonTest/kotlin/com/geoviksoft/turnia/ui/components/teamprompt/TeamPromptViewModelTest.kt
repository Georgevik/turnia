package com.geoviksoft.turnia.ui.components.teamprompt

import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.TeamPromptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamPromptViewModelTest {

    private val team = FakeTeam()
    private val invitations = FakeInvitations()
    private val viewModel = TeamPromptViewModel(team, invitations)

    @Test
    fun joiningAsksForTheJoinSheet() {
        assertTrue(viewModel.answered(TeamPromptChoice.Join))

        assertEquals(1, invitations.requests)
    }

    @Test
    fun creatingDoesNotAskForTheJoinSheet() {
        viewModel.answered(TeamPromptChoice.Create)

        assertEquals(0, invitations.requests)
    }

    @Test
    fun aSecondTapDoesNothing() {
        viewModel.answered(TeamPromptChoice.Join)

        assertFalse(viewModel.answered(TeamPromptChoice.Join))
        assertEquals(1, invitations.requests)
    }

    private class FakeTeam : TeamPromptRepository {
        override val pending = MutableStateFlow(true)
        override suspend fun eventsAdded(count: Int) = Unit
        override suspend fun shown() = Unit
        override fun answered(choice: TeamPromptChoice): Boolean =
            pending.compareAndSet(expect = true, update = false)
    }

    private class FakeInvitations : InvitationLinkRepository {
        var requests = 0
        override val pendingCode: StateFlow<String?> = MutableStateFlow(null)
        override fun opened(link: String) = Unit
        override fun referred(code: String) = Unit
        override fun codeHandled() = Unit
        override val joinSheetRequested: StateFlow<Boolean> = MutableStateFlow(false)
        override fun requestJoinSheet() {
            requests++
        }
        override fun joinSheetOpened() = Unit
    }
}
