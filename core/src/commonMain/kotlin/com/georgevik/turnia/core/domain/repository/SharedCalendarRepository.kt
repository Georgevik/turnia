package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.SharedCalendar
import com.georgevik.turnia.core.domain.model.SharedCalendarError
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import kotlinx.datetime.LocalDate

interface SharedCalendarRepository {

    suspend fun getSharedCalendar(
        ownerId: UserId,
        from: LocalDate,
        to: LocalDate,
    ): Outcome<SharedCalendar, SharedCalendarError>
}
