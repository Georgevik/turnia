package com.geoviksoft.turnia.core.data.group

import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess

/** A blank note is no note: stored as `null`, so clearing one is the same write as never having one. */
internal fun normalizedNote(notes: String?): String? = notes?.trim()?.ifBlank { null }

internal suspend fun saveGroupEventNote(
    notes: String?,
    analytics: Analytics,
    write: suspend (String?) -> Outcome<Unit, GenericFirestoreError>,
): Outcome<Unit, Unit> {
    if (write(normalizedNote(notes)).errorOrNull() != null) return Unit.toFailure()

    analytics.log(AnalyticsEvent.GroupEventNotesSaved)
    return Unit.toSuccess()
}
