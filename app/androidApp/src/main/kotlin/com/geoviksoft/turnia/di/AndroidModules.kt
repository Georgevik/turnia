package com.geoviksoft.turnia.di

import com.geoviksoft.turnia.MainActivityViewModel
import com.geoviksoft.turnia.R
import com.geoviksoft.turnia.ui.system.ads.AdBannerUnitId
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

 val AndroidAppModule = module {
    viewModelOf(::MainActivityViewModel)
    single { AdBannerUnitId(androidContext().getString(R.string.admob_banner_unit_id)) }
}
