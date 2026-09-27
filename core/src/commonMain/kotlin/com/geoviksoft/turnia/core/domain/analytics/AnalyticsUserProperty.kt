package com.geoviksoft.turnia.core.domain.analytics

/**
 * Every user property the app sets, declared here for the same reason as [AnalyticsEvent]: a
 * renamed property starts a new series. Analytics stores every value as a string.
 */
sealed class AnalyticsUserProperty(val name: String, val value: String) {
    /** Groups the user is a member of; a group where they are revoked does not count. */
    class GroupCount(count: Int) : AnalyticsUserProperty("group_count", count.toString())

    /** Admin of at least one of the groups [GroupCount] counts. */
    class IsAdmin(isAdmin: Boolean) : AnalyticsUserProperty("is_admin", isAdmin.value())

    /** The language picked in Preferences, or `system` while the app follows the device. */
    class AppLanguage(tag: String?) : AnalyticsUserProperty("app_language", tag ?: "system")
}
