package com.geoviksoft.turnia.core.data.preferences

import android.content.Context
import org.koin.core.scope.Scope

internal actual fun Scope.preferencesFilePath(fileName: String): String =
    get<Context>().filesDir.resolve(fileName).absolutePath
