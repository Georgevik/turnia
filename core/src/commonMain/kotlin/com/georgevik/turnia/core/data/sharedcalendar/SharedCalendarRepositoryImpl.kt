package com.georgevik.turnia.core.data.sharedcalendar

import com.georgevik.turnia.core.data.datasource.firestorefunctions.SharedCalendarFunction
import com.georgevik.turnia.core.data.sharedcalendar.mappers.SharedCalendarMapper
import com.georgevik.turnia.core.domain.model.SharedCalendar
import com.georgevik.turnia.core.domain.model.SharedCalendarError
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.SharedCalendarRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.map
import kotlinx.datetime.LocalDate

class SharedCalendarRepositoryImpl(
    private val sharedCalendarFunction: SharedCalendarFunction,
    private val sharedCalendarMapper: SharedCalendarMapper,
) : SharedCalendarRepository {

    override suspend fun getSharedCalendar(
        ownerId: UserId,
        from: LocalDate,
        to: LocalDate,
    ): Outcome<SharedCalendar, SharedCalendarError> =
        sharedCalendarFunction.getSharedCalendar(ownerId, from, to)
            .map(sharedCalendarMapper::map)
}
