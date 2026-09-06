package com.georgevik.turnia

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.georgevik.turnia.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class TurniaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(webClientId = BuildConfig.WEB_CLIENT_ID) {
            androidLogger(Level.INFO)
            androidContext(this@TurniaApplication)
        }
        createNotificationChannel()
    }

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
