package com.georgevik.turnia.ui.main.mycalendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.mycalendar.MyCalendarScreen

fun EntryProviderScope<MainRoute>.calendarNavigation() {
    entry<MainRoute.CalendarTab> { MyCalendarScreen() }
}
