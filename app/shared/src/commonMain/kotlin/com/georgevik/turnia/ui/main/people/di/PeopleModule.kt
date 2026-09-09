package com.georgevik.turnia.ui.main.people.di

import com.georgevik.turnia.ui.main.people.PeopleViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Who the user shares their calendar with, and whose they can see. */
val peopleModule: Module = module {
    viewModelOf(::PeopleViewModel)
}
