package com.georgevik.turnia.core.di

import com.georgevik.turnia.core.data.network.FirebaseDataSource
import com.georgevik.turnia.core.domain.repo.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Domain/business layer dependencies (repositories, use cases).
 * Empty for now; it will grow as the Turnia domain does.
 */
val coreModule: Module = module {
    single { FirebaseDataSource(Firebase.auth) }
    single { AuthRepository(get()) }
}
