package com.geoviksoft.turnia.ui.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberTextSharer(): TextSharer {
    val context = LocalContext.current

    return remember(context) {
        object : TextSharer {
            override fun copy(text: String) {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText(null, text))
            }

            override fun share(text: String) {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, null))
            }
        }
    }
}
