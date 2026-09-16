package com.geoviksoft.turnia

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.geoviksoft.turnia.core.system.BuildInfo
import com.geoviksoft.turnia.di.AndroidAppModule
import com.geoviksoft.turnia.di.initKoin
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.logger.Level
import org.koin.core.module.Module

open class TurniaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installFirebase()
        initKoin(
            webClientId = BuildConfig.WEB_CLIENT_ID,
            buildInfo = BuildInfo(isDebug = BuildConfig.DEBUG),
            demo = isDemoMode(this),
        ) {
            androidLogger(Level.INFO)
            androidContext(this@TurniaApplication)
            modules(AndroidAppModule)
        }
        // After start: initKoin loads its own modules after the config block, and the last one wins.
        loadKoinModules(extraModules())
        createNotificationChannel()
    }

    /** Runs before anything reaches Firebase: a call made earlier would go out with no App Check token. */
    protected open fun installFirebase() {
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
    }

    /** Loaded last, so the E2E tests can replace what has no emulator (Remote Config, FCM, consent). */
    protected open fun extraModules(): List<Module> = emptyList()

    /**
     * The channel the manifest points FCM at, created before the first message can arrive.
     *
     * A notification posted against a channel that does not exist is dropped on Android 8+, and
     * the app is not running when a push lands — so this cannot wait for a screen to do it.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
