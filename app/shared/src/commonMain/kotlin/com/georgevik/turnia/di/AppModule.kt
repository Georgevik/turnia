package com.georgevik.turnia.di

import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.navigation.ScreenReporter
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.components.daydetail.DayAddMode
import com.georgevik.turnia.ui.components.daydetail.DayDetailSheetViewModel
import com.georgevik.turnia.ui.group.detail.GroupDetailViewModel
import com.georgevik.turnia.ui.main.MainViewModel
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.georgevik.turnia.ui.main.eventtypes.personal.PersonalEventTypesViewModel
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendarViewModel
import com.georgevik.turnia.ui.main.groups.GroupsViewModel
import com.georgevik.turnia.ui.main.groups.admin.AdminGroupsViewModel
import com.georgevik.turnia.ui.main.mycalendar.MyCalendarViewModel
import com.georgevik.turnia.ui.main.notifications.NotificationsViewModel
import com.georgevik.turnia.ui.main.people.PeopleViewModel
import com.georgevik.turnia.ui.main.profile.MyProfileViewModel
import com.georgevik.turnia.ui.main.settings.SettingsMenuViewModel
import com.georgevik.turnia.ui.main.sharecalendar.ShareCalendarViewModel
import com.georgevik.turnia.ui.root.RootViewModel
import com.georgevik.turnia.ui.signin.SignInViewModel
import com.georgevik.turnia.ui.splash.SplashViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Presentation/UI layer dependencies (ViewModels and their collaborators).
 */
val appModule: Module = module {
    single { ScreenReporter(get()) }
    viewModelOf(::SignInViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::RootViewModel)
    viewModelOf(::MyCalendarViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::SettingsMenuViewModel)
    viewModelOf(::MyProfileViewModel)
    viewModelOf(::ShareCalendarViewModel)
    viewModelOf(::GroupsViewModel)
    viewModelOf(::AdminGroupsViewModel)
    viewModelOf(::PeopleViewModel)
    viewModelOf(::NotificationsViewModel)
    viewModelOf(::PersonalEventTypesViewModel)
    viewModel { (data: EventTypeDetailData) ->
        EventTypeDetailViewModel(data, get(), get())
    }
    viewModel { (key: MainRoute.GroupDetail) ->
        GroupDetailViewModel(GroupId(key.groupId), get())
    }
    viewModel { (date: LocalDate, addMode: DayAddMode) ->
        DayDetailSheetViewModel(date, addMode, get(), get(), get())
    }
    viewModel { (data: ExternalCalendarData) ->
        ExternalCalendarViewModel(data, get(), get(), get())
    }
}
