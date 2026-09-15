package com.geoviksoft.turnia.di

import com.geoviksoft.turnia.ui.group.detail.di.groupDetailModule
import com.geoviksoft.turnia.ui.main.eventtypes.di.eventTypesModule
import com.geoviksoft.turnia.ui.main.groups.di.groupsModule
import com.geoviksoft.turnia.ui.main.mycalendar.di.calendarModule
import com.geoviksoft.turnia.ui.main.people.di.peopleModule
import com.geoviksoft.turnia.ui.main.preferences.di.preferencesModule
import com.geoviksoft.turnia.ui.main.settings.di.settingsModule
import com.geoviksoft.turnia.ui.main.swap.di.swapModule
import com.geoviksoft.turnia.ui.root.di.rootModule
import com.geoviksoft.turnia.ui.system.ads.di.adsModule
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
    swapModule,
    preferencesModule,
    adsModule,
)
