package com.geoviksoft.turnia.ui.main.people.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.people.PeopleScreen

/** "Personas" tab: the calendars other people share with this user. */
fun EntryProviderScope<MainRoute>.peopleNavigation() {
    entry<MainRoute.PeopleTab> { PeopleScreen() }
}
