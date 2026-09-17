package com.geoviksoft.turnia.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope

internal const val PREFERENCES_FILE = "turnia.preferences_pb"

/** Absolute path of [PREFERENCES_FILE] in the app's private storage. */
internal expect fun Scope.preferencesFilePath(): String

internal fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })
