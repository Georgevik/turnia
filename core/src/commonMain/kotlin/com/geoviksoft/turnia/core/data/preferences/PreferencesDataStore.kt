package com.geoviksoft.turnia.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope

internal const val PREFERENCES_FILE = "turnia.preferences_pb"

/**
 * Colleagues' calendars as last fetched, per month. Its own file: it only grows, and the settings
 * file is read on every launch. DataStore refuses a Preferences file not ending in `.preferences_pb`.
 */
internal const val SHARED_CALENDARS_FILE = "turnia.shared_calendars.preferences_pb"

/** The Koin qualifier of the [SHARED_CALENDARS_FILE] store: both are `DataStore<Preferences>`. */
internal const val SHARED_CALENDARS_STORE = "sharedCalendarsStore"

/** Absolute path of [fileName] in the app's private storage. */
internal expect fun Scope.preferencesFilePath(fileName: String): String

internal fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })
