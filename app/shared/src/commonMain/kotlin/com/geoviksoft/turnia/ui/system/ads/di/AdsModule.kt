package com.geoviksoft.turnia.ui.system.ads.di

import com.geoviksoft.turnia.ui.system.ads.AdBannerViewModel
import com.geoviksoft.turnia.ui.system.ads.AdConsent
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val adsModule: Module = module {
    single { AdConsent() }
    viewModelOf(::AdBannerViewModel)
}
