package com.georgevik.turnia.core.di

import com.georgevik.turnia.core.data.config.AppConfigRepositoryImpl
import com.georgevik.turnia.core.data.datasource.firestore.GroupEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupSyncFirestore
import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventTypesFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserSyncFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UsernameFirestore
import com.georgevik.turnia.core.data.datasource.firestore.mappers.GroupMapper
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventMapper
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.datasource.firestore.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.datasource.firestorefunctions.UserProfileFunction
import com.georgevik.turnia.core.data.group.GroupRepositoryImpl
import com.georgevik.turnia.core.data.user.PersonalEventRepositoryImpl
import com.georgevik.turnia.core.data.user.UserProvisioner
import com.georgevik.turnia.core.data.user.UserRepositoryImpl
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.domain.username.UsernameFactory
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Domain/data layer dependencies. It will grow as the Turnia domain does.
 */
val coreModule: Module = module {
    // Outlives every screen: it carries the session and the cache subscriptions.
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { Firebase.firestore }
    single { Firebase.functions }
    single { UserProfileFunction(get()) }
    single { UserPathFirestore(get(), get()) }
    single { UserPrivateFirestore(get()) }
    single { UsernameFirestore(get()) }
    single { UserSyncFirestore(get(), get()) }
    single { GroupSyncFirestore(get(), get()) }
    single { GroupEventFirestore(get(), get()) }
    single { GroupFirestore(get(), get()) }
    single { PersonalEventFirestore(get(), get(), get()) }
    single { PersonalEventTypesFirestore(get(), get(), get()) }
    factory { UserDocumentMapper() }
    factory { UsernameFactory() }
    factory { PersonalEventMapper() }
    factory { PersonalEventTypeDocMapper() }
    factory { GroupMapper() }
    single { UserProvisioner(get(), get(), get(), get()) }
    single<UserRepository> { UserRepositoryImpl(Firebase.auth, get(), get(), get(), get(), get(), get(), get()) }
    single<AppConfigRepository> { AppConfigRepositoryImpl() }
    single<GroupRepository> { GroupRepositoryImpl(get(), get(), get(), get(), get()) }
    single<PersonalEventRepository> { PersonalEventRepositoryImpl(get(), get(), get(), get()) }
}
