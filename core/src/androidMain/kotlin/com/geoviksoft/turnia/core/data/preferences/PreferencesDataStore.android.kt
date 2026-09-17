package com.geoviksoft.turnia.core.data.preferences

import android.content.Context
import org.koin.core.scope.Scope

internal actual fun Scope.preferencesFilePath(): String =
    get<Context>().filesDir.resolve(PREFERENCES_FILE).absolutePath
