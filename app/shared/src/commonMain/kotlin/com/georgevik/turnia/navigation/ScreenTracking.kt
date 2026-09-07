package com.georgevik.turnia.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.navigation.root.routes.RootRoute
import org.koin.compose.koinInject

/**
 * Reports the screen the user is looking at. Nav3 has no destination listener of its own, so the
 * two back stacks call this with their own top entry: the root one, and Main with the tab actually
 * showing.
 */
@Composable
fun TrackScreen(route: NavKey?) {
    val reporter: ScreenReporter = koinInject()
    val screen = route?.let(::screenName)

    LaunchedEffect(screen) {
        screen?.let(reporter::report)
    }
}

/**
 * Written out rather than derived from the class name: the console groups by this string, and a
 * class name changes under a rename and again under release obfuscation, splitting one screen's
 * history into three series that cannot be merged back.
 */
private fun screenName(route: NavKey): String? = when (route) {
    RootRoute.SplashKey -> "splash"
    RootRoute.SignInKey -> "sign_in"
    // Main is a host, not a screen: the tab underneath it reports instead.
    RootRoute.MainKey -> null
    is RootRoute.GroupDetailKey -> "group_detail"
    is RootRoute.EventTypeDetailKey -> "event_type_detail"
    RootRoute.PersonalEventTypesKey -> "personal_event_types"
    RootRoute.MyProfileKey -> "my_profile"
    RootRoute.ShareCalendarKey -> "share_calendar"
    RootRoute.AdminGroupsKey -> "admin_groups"
    RootRoute.NotificationsKey -> "notifications"
    MainRoute.CalendarTab -> "calendar"
    MainRoute.PeopleTab -> "people"
    MainRoute.GroupsTab -> "groups"
    MainRoute.ChangesTab -> "changes"
    MainRoute.SettingsMenuTab -> "settings"
    is MainRoute.ExternalCalendar -> "external_calendar"
    else -> null
}
