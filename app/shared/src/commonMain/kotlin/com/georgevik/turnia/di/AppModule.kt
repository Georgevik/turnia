package com.georgevik.turnia.di

import com.georgevik.turnia.core.domain.model.CalendarKind
import com.georgevik.turnia.ui.components.daydetail.DayDetailsSheetViewModel
import com.georgevik.turnia.ui.main.MainViewModel
import com.georgevik.turnia.ui.main.eventtypes.EventMasterViewModel
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendarViewModel
import com.georgevik.turnia.ui.main.group.calendarlist.CalendarListViewModel
import com.georgevik.turnia.ui.main.mycalendar.MyCalendarViewModel
import com.georgevik.turnia.ui.root.RootViewModel
import com.georgevik.turnia.ui.signin.SignInViewModel
import com.georgevik.turnia.ui.splash.SplashViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlinx.datetime.LocalDate

/**
 * Presentation/UI layer dependencies (ViewModels and their collaborators).
 */
val appModule: Module = module {
    viewModelOf(::SignInViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::RootViewModel)
    viewModelOf(::MyCalendarViewModel)
    viewModelOf(::CalendarListViewModel)
    viewModelOf(::MainViewModel)
    viewModel { (groupId: String, groupName: String) ->
        EventMasterViewModel(groupId, groupName, get(), get())
    }
    viewModel { (typeId: String?, groupId: String?) ->
        EventTypeDetailViewModel(typeId, groupId, get(), get())
    }
    viewModel { (date: LocalDate) ->
        DayDetailsSheetViewModel(date, get(), get())
    }
    viewModel { (id: String, name: String, kind: CalendarKind) ->
        ExternalCalendarViewModel(id, name, kind)
    }
}
