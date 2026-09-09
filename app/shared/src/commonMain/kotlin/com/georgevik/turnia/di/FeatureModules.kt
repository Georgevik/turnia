package com.georgevik.turnia.di

import com.georgevik.turnia.ui.group.detail.di.groupDetailModule
import com.georgevik.turnia.ui.main.eventtypes.di.eventTypesModule
import com.georgevik.turnia.ui.main.groups.di.groupsModule
import com.georgevik.turnia.ui.main.mycalendar.di.calendarModule
import com.georgevik.turnia.ui.main.notifications.di.notificationsModule
import com.georgevik.turnia.ui.main.people.di.peopleModule
import com.georgevik.turnia.ui.main.settings.di.settingsModule
import com.georgevik.turnia.ui.root.di.rootModule
import org.koin.core.module.Module

/**
 * The presentation layer, one module per feature. Each lives next to the feature it wires, the way
 * a feature's `navigation` does, so adding a screen means touching that feature and nothing else.
 */
val featureModules: List<Module> = listOf(
    rootModule,
    calendarModule,
    groupsModule,
    groupDetailModule,
    eventTypesModule,
    peopleModule,
    settingsModule,
    notificationsModule,
)
