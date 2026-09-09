package com.geoviksoft.turnia.ui.main.mycalendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.mycalendar.MyCalendarScreen

fun EntryProviderScope<MainRoute>.calendarNavigation() {
    entry<MainRoute.CalendarTab> { MyCalendarScreen() }
}
