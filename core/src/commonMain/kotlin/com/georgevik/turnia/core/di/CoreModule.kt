package com.georgevik.turnia.core.di

import com.georgevik.turnia.core.data.network.FirebaseDataSource
import com.georgevik.turnia.core.domain.repository.UserRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.GlobalScope
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Domain/data layer dependencies. It will grow as the Turnia domain does.
 */
val coreModule: Module = module {
    single { FirebaseDataSource(Firebase.auth) }
    single { UserRepository(Firebase.auth, GlobalScope) }
}
