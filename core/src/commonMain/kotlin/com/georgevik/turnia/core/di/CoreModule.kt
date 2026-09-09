package com.georgevik.turnia.core.di

import com.georgevik.turnia.core.data.analytics.AnalyticsImpl
import com.georgevik.turnia.core.data.config.AppConfigRepositoryImpl
import com.georgevik.turnia.core.data.datasource.firestore.GroupEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupJoinRequestFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupSyncFirestore
import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.PersonalEventTypesFirestore
import com.georgevik.turnia.core.data.datasource.firestore.RevokedGroupFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserSyncFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UsernameFirestore
import com.georgevik.turnia.core.data.datasource.firestorefunctions.GroupFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.GroupMembershipFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.SharedCalendarFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.UserProfileFunction
import com.georgevik.turnia.core.data.group.GroupFactory
import com.georgevik.turnia.core.data.group.GroupRepositoryImpl
import com.georgevik.turnia.core.data.group.InvitationCodeFactory
import com.georgevik.turnia.core.data.group.mappers.GroupErrorMapper
import com.georgevik.turnia.core.data.group.mappers.GroupMapper
import com.georgevik.turnia.core.data.notification.NotificationRepositoryImpl
import com.georgevik.turnia.core.data.sharedcalendar.SharedCalendarRepositoryImpl
import com.georgevik.turnia.core.data.sharedcalendar.mappers.SharedCalendarErrorMapper
import com.georgevik.turnia.core.data.sharedcalendar.mappers.SharedCalendarMapper
import com.georgevik.turnia.core.data.user.PersonalEventRepositoryImpl
import com.georgevik.turnia.core.data.user.UserProvisioner
import com.georgevik.turnia.core.data.user.UserRepositoryImpl
import com.georgevik.turnia.core.data.user.delegate.FcmDelegateImpl
import com.georgevik.turnia.core.data.user.mappers.PersonalEventMapper
import com.georgevik.turnia.core.data.user.mappers.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.user.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.user.mappers.UsernameErrorMapper
import com.georgevik.turnia.core.domain.analytics.Analytics
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import com.georgevik.turnia.core.domain.repository.FcmDelegate
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.NotificationRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.SharedCalendarRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.domain.username.UsernameFactory
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The region the callables are deployed to, next to Firestore. Naming it is not optional: the SDK
 * calls `us-central1` when nothing says otherwise, and a function that lives elsewhere is simply
 * not found there.
 */
private const val FUNCTIONS_REGION = "europe-southwest1"

/**
 * Domain/data layer dependencies. It will grow as the Turnia domain does.
 */
val coreModule: Module = module {
    // Outlives every screen: it carries the session and the cache subscriptions.
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { Firebase.firestore }
    single { Firebase.functions(FUNCTIONS_REGION) }
    single { Firebase.messaging }
    single { Firebase.analytics }
    single<Analytics> { AnalyticsImpl(get()) }
    single { UserProfileFunction(get(), get()) }
    single { UserPathFirestore(get(), get(), get()) }
    single { UserPrivateFirestore(get(), get()) }
    single { UsernameFirestore(get(), get(), get()) }
    single { UserSyncFirestore(get(), get()) }
    single { GroupSyncFirestore(get(), get()) }
    single { GroupEventFirestore(get(), get()) }
    single { GroupFirestore(get(), get(), get()) }
    single { GroupJoinRequestFirestore(get()) }
    single { RevokedGroupFirestore(get(), get(), get()) }
    single { GroupMembershipFunction(get(), get(), get()) }
    single { GroupFunction(get(), get()) }
    single { SharedCalendarFunction(get(), get()) }
    single { PersonalEventFirestore(get(), get(), get()) }
    single { PersonalEventTypesFirestore(get(), get(), get(), get()) }
    factory { UserDocumentMapper() }
    factory { UsernameFactory() }
    factory { InvitationCodeFactory() }
    factory { GroupFactory() }
    factory { PersonalEventMapper() }
    factory { PersonalEventTypeDocMapper() }
    factory { GroupMapper() }
    factory { GroupErrorMapper() }
    factory { UsernameErrorMapper() }
    factory { SharedCalendarMapper() }
    factory { SharedCalendarErrorMapper() }
    single { UserProvisioner(get(), get(), get(), get()) }
    single<FcmDelegate> { FcmDelegateImpl(get(), get()) }
    single<UserRepository> {
        UserRepositoryImpl(
            Firebase.auth,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single<AppConfigRepository> { AppConfigRepositoryImpl() }
    single<NotificationRepository> { NotificationRepositoryImpl() }
    single<GroupRepository> {
        GroupRepositoryImpl(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get()
        )
    }
    single<PersonalEventRepository> { PersonalEventRepositoryImpl(get(), get(), get(), get()) }
    single<SharedCalendarRepository> { SharedCalendarRepositoryImpl(get(), get()) }
}
