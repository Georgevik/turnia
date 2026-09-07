package com.georgevik.turnia.core.data.analytics

import com.georgevik.turnia.core.domain.analytics.Analytics
import com.georgevik.turnia.core.domain.analytics.AnalyticsEvent
import com.georgevik.turnia.core.domain.model.UserId
import dev.gitlive.firebase.analytics.FirebaseAnalytics

class AnalyticsImpl(private val analytics: FirebaseAnalytics) : Analytics {

    override fun log(event: AnalyticsEvent) {
        analytics.logEvent(event.name, event.parameters)
    }

    override fun setUser(userId: UserId?) {
        analytics.setUserId(userId?.value)
    }
}
