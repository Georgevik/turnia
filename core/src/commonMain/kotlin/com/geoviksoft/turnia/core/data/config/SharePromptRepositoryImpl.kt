package com.geoviksoft.turnia.core.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.SharePromptAnswer
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import com.geoviksoft.turnia.core.system.outcomeCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [SharePromptRepository] over the device's settings file: the count of events added and the value
 * of the last milestone shown. The value rather than its position, so a milestone list edited in the
 * console can neither bring back one already passed nor skip one wrongly.
 *
 * A milestone is only spent once the prompt is on screen, not when it is due: an app closed with the
 * prompt still waiting offers it again on the next event instead of losing it.
 */
class SharePromptRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val appConfigRepository: AppConfigRepository,
    private val analytics: Analytics,
) : SharePromptRepository {

    private val _pending = MutableStateFlow<SharePrompt?>(null)
    override val pending: StateFlow<SharePrompt?> = _pending.asStateFlow()

    override suspend fun eventAdded(kind: EventKind) {
        var added = 0
        var lastShown = 0
        outcomeCatching(TAG, mapError = {}) {
            dataStore.edit { preferences ->
                added = (preferences[EVENTS_ADDED] ?: 0) + 1
                lastShown = preferences[LAST_MILESTONE] ?: 0
                preferences[EVENTS_ADDED] = added
            }
        }

        // A device that counted events before this report existed is past its first: it never logs.
        if (added == 1) analytics.log(AnalyticsEvent.FirstEventAdded(kind))

        val flags = appConfigRepository.featureFlags.value
        if (!flags.sharePromptActive) return

        // Several milestones passed at once (the flag was off, or one was added below the count)
        // make one prompt, not a string of them.
        val milestone = flags.sharePromptMilestones.lastOrNull { it in (lastShown + 1)..added } ?: return
        _pending.value = SharePrompt(kind.audience, milestone)
    }

    override suspend fun shown(prompt: SharePrompt) {
        var first = false
        outcomeCatching(TAG, mapError = {}) {
            dataStore.edit { preferences ->
                // Recomposing, or a rotation, shows the same prompt again: it is logged once.
                first = (preferences[LAST_MILESTONE] ?: 0) < prompt.milestone
                if (first) preferences[LAST_MILESTONE] = prompt.milestone
            }
        }
        if (first) analytics.log(AnalyticsEvent.SharePromptShown(prompt))
    }

    override fun answered(prompt: SharePrompt, answer: SharePromptAnswer): Boolean {
        if (!_pending.compareAndSet(prompt, null)) return false
        analytics.log(
            when (answer) {
                SharePromptAnswer.Shared -> AnalyticsEvent.SharePromptShared(prompt)
                SharePromptAnswer.Dismissed -> AnalyticsEvent.SharePromptDismissed(prompt)
            }
        )
        return true
    }

    private companion object {
        const val TAG = "SharePromptRepository"
        val EVENTS_ADDED = intPreferencesKey("share_prompt_events_added")
        val LAST_MILESTONE = intPreferencesKey("share_prompt_last_milestone")
    }
}
