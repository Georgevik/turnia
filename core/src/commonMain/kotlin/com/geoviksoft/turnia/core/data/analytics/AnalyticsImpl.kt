package com.geoviksoft.turnia.core.data.analytics

import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsUserProperty
import com.geoviksoft.turnia.core.domain.model.UserId
import dev.gitlive.firebase.analytics.FirebaseAnalytics

class AnalyticsImpl(private val analytics: FirebaseAnalytics) : Analytics {

    override fun log(event: AnalyticsEvent) {
        analytics.logEvent(event.name, event.parameters)
    }

    override fun setUserProperty(property: AnalyticsUserProperty) {
        analytics.setUserProperty(property.name, property.value)
    }

    override fun setUser(userId: UserId?) {
        analytics.setUserId(userId?.value)
    }
}
