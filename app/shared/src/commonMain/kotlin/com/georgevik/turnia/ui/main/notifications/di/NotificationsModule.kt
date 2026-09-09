package com.georgevik.turnia.ui.main.notifications.di

import com.georgevik.turnia.ui.main.notifications.NotificationsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val notificationsModule: Module = module {
    viewModelOf(::NotificationsViewModel)
}
