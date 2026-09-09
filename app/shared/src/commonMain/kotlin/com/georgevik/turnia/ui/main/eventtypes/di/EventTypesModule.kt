package com.georgevik.turnia.ui.main.eventtypes.di

import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.georgevik.turnia.ui.main.eventtypes.personal.PersonalEventTypesViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Event types, personal and a group's alike: the detail screen serves both. */
val eventTypesModule: Module = module {
    viewModelOf(::PersonalEventTypesViewModel)
    viewModel { (data: EventTypeDetailData) ->
        EventTypeDetailViewModel(data, get(), get())
    }
}
