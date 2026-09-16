package com.geoviksoft.turnia.ui.system

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
actual fun rememberStoreReview(): StoreReview {
    val context = LocalContext.current

    return remember(context) {
        object : StoreReview {
            override val store = AppStore.GOOGLE_PLAY

            override fun open() {
                val id = context.packageName
                try {
                    context.startActivity(view("market://details?id=$id"))
                } catch (_: ActivityNotFoundException) {
                    // A device without Play (an emulator image, a de-Googled phone) gets the web page.
                    context.startActivity(view("https://play.google.com/store/apps/details?id=$id"))
                }
            }

            private fun view(uri: String) =
                Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
