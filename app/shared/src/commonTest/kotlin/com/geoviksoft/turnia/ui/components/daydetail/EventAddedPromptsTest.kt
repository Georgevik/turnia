package com.geoviksoft.turnia.ui.components.daydetail

import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.SharePromptAnswer
import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import com.geoviksoft.turnia.core.domain.repository.TeamPromptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EventAddedPromptsTest {

    private val calls = mutableListOf<String>()
    private val share = FakeShare(calls)
    private val team = FakeTeam(calls)
    private val prompts = EventAddedPrompts(share, team)

    @Test
    fun theShareCountReachesTheTeamPrompt() = runTest {
        share.count = 5

        prompts.added(EventKind.OneOff)

        assertEquals(listOf("share:OneOff", "team:5"), calls)
    }

    @Test
    fun aCountThatWasNotWrittenIsNotPassedOn() = runTest {
        share.count = 0

        prompts.added(EventKind.Typed)

        assertEquals(listOf("share:Typed"), calls)
    }

    private class FakeShare(private val calls: MutableList<String>) : SharePromptRepository {
        var count = 1
        override val pending: StateFlow<SharePrompt?> = MutableStateFlow(null)
        override suspend fun eventAdded(kind: EventKind): Int {
            calls += "share:$kind"
            return count
        }
        override suspend fun shown(prompt: SharePrompt) = Unit
        override fun answered(prompt: SharePrompt, answer: SharePromptAnswer) = false
    }

    private class FakeTeam(private val calls: MutableList<String>) : TeamPromptRepository {
        override val pending: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun eventsAdded(count: Int) {
            calls += "team:$count"
        }
        override suspend fun shown() = Unit
        override fun answered(choice: TeamPromptChoice) = false
    }
}
