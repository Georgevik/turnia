package com.geoviksoft.turnia.ui.system.photo

import okio.Path

/** Where [ProfilePhotoCache] keeps its files: the platform's cache directory, kept across launches. */
internal expect fun photoCacheDirectory(): Path
