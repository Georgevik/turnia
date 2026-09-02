package com.georgevik.turnia.core.di

import com.georgevik.turnia.core.data.config.AppConfigRepositoryImpl
import com.georgevik.turnia.core.data.group.GroupRepositoryImpl
import com.georgevik.turnia.core.data.network.FirebaseDataSource
import com.georgevik.turnia.core.data.user.PersonalEventRepositoryImpl
import com.georgevik.turnia.core.data.user.UserDocumentMapper
import com.georgevik.turnia.core.data.user.UserRepositoryImpl
import com.georgevik.turnia.core.data.user.datasource.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.user.datasource.UserPathFirestore
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.GlobalScope
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Domain/data layer dependencies. It will grow as the Turnia domain does.
 */
val coreModule: Module = module {
    single { FirebaseDataSource(Firebase.auth) }
    single { Firebase.firestore }
    single { UserPathFirestore(get(), get(), get()) }
    factory { UserDocumentMapper() }
    factory { PersonalEventTypeDocMapper() }
    single<UserRepository> { UserRepositoryImpl(Firebase.auth, get(), get(), GlobalScope) }
    single<AppConfigRepository> { AppConfigRepositoryImpl() }
    single<GroupRepository> { GroupRepositoryImpl() }
    single<PersonalEventRepository> { PersonalEventRepositoryImpl(get(), get()) }
}
