package com.geoviksoft.turnia.core.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.geoviksoft.turnia.core.data.user.GroupMembership
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.TeamPromptRepository
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * [TeamPromptRepository] over the device's settings file. The prompt settles for good once it has
 * been on screen, or once the server says the user has a group: someone who has been in one already
 * knows what groups are for, so the membership read is never repeated.
 *
 * Nothing but the settled flag is stored: a prompt that became due and never reached the screen is
 * decided again on the next event added, the same way a share prompt's milestone waits.
 */
class TeamPromptRepositoryImpl(
    private val session: StateFlow<UserSession>,
    private val dataStore: DataStore<Preferences>,
    private val appConfigRepository: AppConfigRepository,
    private val membership: GroupMembership,
    private val analytics: Analytics,
) : TeamPromptRepository {

    private val _pending = MutableStateFlow(false)
    override val pending: StateFlow<Boolean> = _pending.asStateFlow()

    override suspend fun eventsAdded(count: Int) {
        val flags = appConfigRepository.featureFlags.value
        if (!flags.teamPromptActive || count < flags.teamPromptThreshold) return
        if (_pending.value || isSettled()) return

        val uid = (session.value as? UserSession.Authenticated)?.user?.id ?: return
        // An empty cache proves nothing on a fresh install: without the server's word, wait.
        val hasGroup = membership.serverHasAnyGroup(uid).valueOrNull() ?: return
        if (hasGroup) settle() else _pending.value = true
    }

    override suspend fun shown() {
        settle()
    }

    override fun answered(choice: TeamPromptChoice): Boolean {
        return _pending.compareAndSet(expect = true, update = false)
    }

    private suspend fun isSettled(): Boolean =
        outcomeCatching(TAG, mapError = {}) { dataStore.data.first()[SETTLED] == true }
            .valueOrNull() ?: false

    /** Returns whether this call is the one that settled it. */
    private suspend fun settle(): Boolean {
        var settledNow = false
        outcomeCatching(TAG, mapError = {}) {
            dataStore.edit { preferences ->
                settledNow = preferences[SETTLED] != true
                preferences[SETTLED] = true
            }
        }
        return settledNow
    }

    private companion object {
        const val TAG = "TeamPromptRepository"
        val SETTLED = booleanPreferencesKey("team_prompt_settled")
    }
}
