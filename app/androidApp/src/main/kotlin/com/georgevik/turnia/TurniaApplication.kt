package com.georgevik.turnia

import android.app.Application
import com.georgevik.turnia.di.initKoin
import com.georgevik.turnia.interfaces.androidAuthModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class TurniaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger(Level.INFO)
            androidContext(this@TurniaApplication)
            modules(androidAuthModule)
        }
    }
}
