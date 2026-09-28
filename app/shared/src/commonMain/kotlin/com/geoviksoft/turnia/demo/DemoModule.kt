package com.geoviksoft.turnia.demo

import com.geoviksoft.turnia.core.domain.repository.ShiftSetupRepository
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.FcmDelegate
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.SharedCalendarRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * The app on made-up data, for store screenshots and demos: loaded after every other module, it
 * replaces the repositories that would reach Firebase with in-memory ones. Only a debug build can
 * switch it on — see where `initKoin` is called on each platform.
 */
val demoModule: Module = module {
    single { DemoWorld(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    single<UserRepository> { DemoUserRepository() }
    single<FcmDelegate> { get<UserRepository>() }
    single<GroupRepository> { DemoGroupRepository(get()) }
    single<PersonalEventRepository> { DemoPersonalEventRepository(get()) }
    single<SharedCalendarRepository> { DemoSharedCalendarRepository(get()) }
    single<Analytics> { DemoAnalytics }
    single<AppConfigRepository> { DemoAppConfigRepository() }
    single<ShiftSetupRepository> { DemoShiftSetupRepository(get()) }
}
