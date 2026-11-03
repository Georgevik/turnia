package com.geoviksoft.turnia.ui.system.photo

import okio.FileSystem
import okio.Path

// On Android the JVM's temporary directory is the app's own cache directory.
internal actual fun photoCacheDirectory(): Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "profile_photos"
