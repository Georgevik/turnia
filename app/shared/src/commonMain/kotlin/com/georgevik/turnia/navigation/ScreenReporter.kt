package com.georgevik.turnia.navigation

import com.georgevik.turnia.core.domain.analytics.Analytics
import com.georgevik.turnia.core.domain.analytics.AnalyticsEvent

/**
 * Reports a screen only when it is not the one already showing.
 *
 * The state is process-scoped and not remembered in composition on purpose. Remembering it per call
 * site gets both halves wrong: the root stack and Main each keep their own, so a screen reached from
 * the other stack looks new; and a saveable one is restored along with the nav entry, which would
 * swallow the return to a tab the user actually navigated back to. One instance, one last value, and
 * the only thing it collapses is the same screen twice in a row — which is what an Activity
 * recreated by a theme or locale change produces.
 */
class ScreenReporter(private val analytics: Analytics) {

    private var lastReported: String? = null

    fun report(screen: String) {
        if (screen == lastReported) return

        lastReported = screen
        analytics.log(AnalyticsEvent.ScreenView(screen))
    }
}
