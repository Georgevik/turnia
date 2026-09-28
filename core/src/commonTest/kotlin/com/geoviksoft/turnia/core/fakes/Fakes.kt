package com.geoviksoft.turnia.core.fakes

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsUserProperty
import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

class RecordingAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    fun named(name: String) = events.filter { it.name == name }

    override fun log(event: AnalyticsEvent) {
        events += event
    }

    override fun setUserProperty(property: AnalyticsUserProperty) = Unit

    override fun setUser(userId: UserId?) = Unit
}

class FakeAppConfigRepository(flags: FeatureFlags = defaultFlags) : AppConfigRepository {
    override val featureFlags: StateFlow<FeatureFlags> = MutableStateFlow(flags)
    var onboardingSeen = false
    var shiftSetupSettled = false

    override suspend fun refreshFeatureFlags(): FeatureFlags = featureFlags.value
    override suspend fun isOnboardingSeen(): Boolean = onboardingSeen
    override suspend fun setOnboardingSeen(seen: Boolean) {
        onboardingSeen = seen
    }

    companion object {
        val defaultFlags = FeatureFlags(
            minActionsToEnableAds = -1,
            invitationCodeLength = 6,
            enableSubscription = false,
            supportEmail = "",
        )
    }
}
