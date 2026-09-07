package com.georgevik.turnia.core.domain.analytics

/**
 * Every event name and parameter key the app reports. They are declared here rather than written at
 * each call site because the console groups by the literal string: renaming one starts a fresh
 * series and leaves the history behind it orphaned, with no way to stitch the two back together.
 */
sealed class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, Any> = emptyMap(),
) {
    /** `screen_view` and `screen_name` are Google's own names; the console reads no others. */
    class ScreenView(screen: String) : AnalyticsEvent(
        name = "screen_view",
        parameters = mapOf("screen_name" to screen),
    )

    data object GroupCreated : AnalyticsEvent("group_created")

    data object GroupEventCreated : AnalyticsEvent("group_event_created")

    data object JoinRequested : AnalyticsEvent("join_group_requested")

    /** Google's own `join_group`: the request was accepted and the user is now a member. */
    data object JoinAccepted : AnalyticsEvent("join_group")
}
