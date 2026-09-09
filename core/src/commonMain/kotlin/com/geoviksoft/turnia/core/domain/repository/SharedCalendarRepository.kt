package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.SharedCalendar
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import kotlinx.datetime.LocalDate

interface SharedCalendarRepository {

    suspend fun getSharedCalendar(
        ownerId: UserId,
        from: LocalDate,
        to: LocalDate,
    ): Outcome<SharedCalendar, SharedCalendarError>
}
