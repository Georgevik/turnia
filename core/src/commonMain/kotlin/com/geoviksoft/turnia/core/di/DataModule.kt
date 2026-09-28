package com.geoviksoft.turnia.core.di

import com.geoviksoft.turnia.core.data.ads.AdRepositoryImpl
import com.geoviksoft.turnia.core.data.analytics.AnalyticsImpl
import com.geoviksoft.turnia.core.data.config.AppConfigRepositoryImpl
import com.geoviksoft.turnia.core.data.config.DeviceSettings
import com.geoviksoft.turnia.core.data.config.RemoteConfigService
import com.geoviksoft.turnia.core.data.config.SharePromptRepositoryImpl
import com.geoviksoft.turnia.core.data.config.TeamPromptRepositoryImpl
import com.geoviksoft.turnia.core.data.user.FirestoreGroupMembership
import com.geoviksoft.turnia.core.data.user.GroupMembership
import com.geoviksoft.turnia.core.domain.repository.TeamPromptRepository
import com.geoviksoft.turnia.core.domain.repository.ShiftSetupRepository
import com.geoviksoft.turnia.core.data.user.ShiftSetupRepositoryImpl
import com.geoviksoft.turnia.core.data.user.FirestoreAccountContents
import com.geoviksoft.turnia.core.data.user.AccountContents
import com.geoviksoft.turnia.core.data.config.mappers.SharePromptMilestonesMapper
import com.geoviksoft.turnia.core.data.datasource.firestore.GroupEventFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.GroupFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.GroupJoinRequestFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.GroupSyncFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.PersonalEventFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.PersonalEventTypesFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.PersonalOneOffEventFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.RevokedGroupFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UserPathFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UserSyncFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UsernameFirestore
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.GroupEventFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.GroupFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.GroupMembershipFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.SharedCalendarFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.UserProfileFunction
import com.geoviksoft.turnia.core.data.group.GroupFactory
import com.geoviksoft.turnia.core.data.group.GroupRepositoryImpl
import com.geoviksoft.turnia.core.data.group.InvitationCodeFactory
import com.geoviksoft.turnia.core.data.group.mappers.GroupErrorMapper
import com.geoviksoft.turnia.core.data.group.mappers.GroupMapper
import com.geoviksoft.turnia.core.data.invitation.InvitationLinkRepositoryImpl
import com.geoviksoft.turnia.core.data.notification.NotificationRepositoryImpl
import com.geoviksoft.turnia.core.data.preferences.PREFERENCES_FILE
import com.geoviksoft.turnia.core.data.preferences.SHARED_CALENDARS_FILE
import com.geoviksoft.turnia.core.data.preferences.SHARED_CALENDARS_STORE
import com.geoviksoft.turnia.core.data.preferences.createPreferencesDataStore
import com.geoviksoft.turnia.core.data.preferences.preferencesFilePath
import com.geoviksoft.turnia.core.data.sharedcalendar.SharedCalendarCache
import com.geoviksoft.turnia.core.data.sharedcalendar.SharedCalendarRepositoryImpl
import com.geoviksoft.turnia.core.data.sharedcalendar.mappers.SharedCalendarErrorMapper
import com.geoviksoft.turnia.core.data.sharedcalendar.mappers.SharedCalendarMapper
import com.geoviksoft.turnia.core.data.user.PersonalEventRepositoryImpl
import com.geoviksoft.turnia.core.data.user.UserProvisioner
import com.geoviksoft.turnia.core.data.user.UserRepositoryImpl
import com.geoviksoft.turnia.core.data.user.delegate.FcmDelegateImpl
import com.geoviksoft.turnia.core.data.user.mappers.PersonalEventMapper
import com.geoviksoft.turnia.core.data.user.mappers.PersonalEventTypeDocMapper
import com.geoviksoft.turnia.core.data.user.mappers.UserDocumentMapper
import com.geoviksoft.turnia.core.data.user.mappers.UsernameErrorMapper
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.FcmDelegate
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.NotificationRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import com.geoviksoft.turnia.core.domain.repository.SharedCalendarRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.domain.username.UsernameFactory
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestoreSettings
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.persistentCacheSettings
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.messaging.messaging
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * The region the callables are deployed to, next to Firestore. Naming it is not optional: the SDK
 * calls `us-central1` when nothing says otherwise, and a function that lives elsewhere is simply
 * not found there.
 */
private const val FUNCTIONS_REGION = "europe-southwest1"

/**
 * Everything under `core`: the domain's own collaborators, and the data layer that serves them —
 * datasources, mappers, factories, and the repositories bound to their interfaces.
 */
val dataModule: Module = module {
    // Outlives every screen: it carries the session and the cache subscriptions.
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single {
        Firebase.firestore.apply {
            // Unbounded, not the SDK's 100 MB LRU: past events outlive the server's retention window
            // only here, and a garbage-collected month would be gone for good. Set before any read,
            // which is why nothing else may touch `Firebase.firestore` directly.
            settings = firestoreSettings {
                cacheSettings = persistentCacheSettings {
                    sizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
                }
            }
        }
    }
    single { Firebase.functions(FUNCTIONS_REGION) }
    single { Firebase.messaging }
    single { Firebase.analytics }
    single { Firebase.remoteConfig }
    single<Analytics> { AnalyticsImpl(get()) }
    single { createPreferencesDataStore(preferencesFilePath(PREFERENCES_FILE)) }
    single(named(SHARED_CALENDARS_STORE)) {
        createPreferencesDataStore(preferencesFilePath(SHARED_CALENDARS_FILE))
    }

    // Datasources.
    single { UserProfileFunction(get(), get()) }
    single { UserPathFirestore(get(), get(), get(), get(), get()) }
    single { UserPrivateFirestore(get(), get(), get()) }
    single { UsernameFirestore(get(), get()) }
    single { UserSyncFirestore(get(), get()) }
    single { GroupSyncFirestore(get(), get()) }
    single { GroupEventFirestore(get(), get()) }
    single { GroupFirestore(get(), get(), get(), get()) }
    single { GroupJoinRequestFirestore(get(), get()) }
    single { RevokedGroupFirestore(get(), get(), get()) }
    single { GroupMembershipFunction(get(), get(), get()) }
    single { GroupFunction(get(), get()) }
    single { GroupEventFunction(get(), get()) }
    single { SharedCalendarFunction(get(), get()) }
    single { PersonalEventFirestore(get(), get(), get()) }
    single { PersonalOneOffEventFirestore(get(), get(), get()) }
    single { PersonalEventTypesFirestore(get(), get(), get(), get()) }

    // Domain rules that depend on nothing outside the domain.
    factory { UsernameFactory() }

    // Mappers and factories, one set per context.
    factory { GroupMapper() }
    factory { GroupErrorMapper() }
    factory { GroupFactory() }
    factory { InvitationCodeFactory() }
    factory { UserDocumentMapper() }
    factory { UsernameErrorMapper() }
    factory { PersonalEventMapper() }
    factory { PersonalEventTypeDocMapper() }
    factory { SharedCalendarMapper() }
    factory { SharePromptMilestonesMapper() }
    factory { SharedCalendarErrorMapper() }

    // Repositories.
    single { UserProvisioner(get(), get(), get(), get(), get()) }
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
    single { RemoteConfigService(get(), get(), get()) }
    single { DeviceSettings(get()) }
    single<AppConfigRepository> { AppConfigRepositoryImpl(get(), get()) }
    single<SharePromptRepository> { SharePromptRepositoryImpl(get(), get(), get()) }
    single<AccountContents> { FirestoreAccountContents(get(), get()) }
    single<GroupMembership> { FirestoreGroupMembership(get()) }
    single<TeamPromptRepository> {
        TeamPromptRepositoryImpl(get<UserRepository>().userSession, get(), get(), get(), get())
    }
    single<ShiftSetupRepository> {
        ShiftSetupRepositoryImpl(get<UserRepository>().userSession, get(), get(), get(), get(), get(), get())
    }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<InvitationLinkRepository> { InvitationLinkRepositoryImpl(get()) }
    single<GroupRepository> {
        GroupRepositoryImpl(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get()
        )
    }
    single<PersonalEventRepository> { PersonalEventRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single { SharedCalendarCache(get(named(SHARED_CALENDARS_STORE))) }
    single<SharedCalendarRepository> { SharedCalendarRepositoryImpl(get(), get(), get(), get(), get()) }
    single<AdRepository> { AdRepositoryImpl(get(), get(), get()) }
}
