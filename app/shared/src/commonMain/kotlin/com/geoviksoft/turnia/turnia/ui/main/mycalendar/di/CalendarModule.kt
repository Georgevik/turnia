package com.geoviksoft.turnia.ui.main.mycalendar.di

import com.geoviksoft.turnia.navigation.main.routes.ExternalCalendarData
import com.geoviksoft.turnia.ui.components.daydetail.DayAddMode
import com.geoviksoft.turnia.ui.components.daydetail.DayDetailSheetViewModel
import com.geoviksoft.turnia.ui.main.group.externalcalendar.ExternalCalendarViewModel
import com.geoviksoft.turnia.ui.main.mycalendar.MyCalendarViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Every calendar: the user's own, a group's, a colleague's, and the day sheet all three open.
 */
val calendarModule: Module = module {
    viewModelOf(::MyCalendarViewModel)
    viewModel { (data: ExternalCalendarData) ->
        ExternalCalendarViewModel(data, get(), get(), get())
    }
    viewModel { (date: LocalDate, addMode: DayAddMode) ->
        DayDetailSheetViewModel(date, addMode, get(), get(), get())
    }
}
