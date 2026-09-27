package com.geoviksoft.turnia.core.domain.analytics

import com.geoviksoft.turnia.core.domain.model.UserId

interface Analytics {
    fun log(event: AnalyticsEvent)

    fun setUserProperty(property: AnalyticsUserProperty)

    /** Null on sign-out, which unbinds the reports from the account that just left. */
    fun setUser(userId: UserId?)
}
