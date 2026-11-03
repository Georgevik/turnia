package com.geoviksoft.turnia.ui.system.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The system Photo Picker, images only; below Android 11 it falls back to the document picker. */
@Composable
actual fun rememberPhotoPicker(): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { arrayOfNulls<(ByteArray?) -> Unit>(1) }

    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        val onPicked = pending[0] ?: return@rememberLauncherForActivityResult
        pending[0] = null
        if (uri == null) return@rememberLauncherForActivityResult onPicked(null)

        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            onPicked(bytes)
        }
    }

    return remember(launcher) {
        PhotoPicker { onPicked ->
            pending[0] = onPicked
            launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }
}
