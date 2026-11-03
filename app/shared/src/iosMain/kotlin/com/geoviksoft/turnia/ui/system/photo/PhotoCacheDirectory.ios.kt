package com.geoviksoft.turnia.ui.system.photo

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

// Caches, not tmp: the system empties tmp whenever it likes, and every photo would be downloaded again.
internal actual fun photoCacheDirectory(): Path {
    val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() as? String
    return (caches?.toPath() ?: FileSystem.SYSTEM_TEMPORARY_DIRECTORY) / "profile_photos"
}
