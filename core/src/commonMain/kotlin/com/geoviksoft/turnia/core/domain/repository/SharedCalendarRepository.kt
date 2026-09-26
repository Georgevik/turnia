package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.SharedCalendar
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth

interface SharedCalendarRepository {

    /**
     * [ownerId]'s calendar around [month]: what the device already has first, then again every time
     * the owner changes something in it, for as long as it is collected.
     */
    fun sharedCalendar(
        ownerId: UserId,
        month: YearMonth,
    ): Flow<Outcome<SharedCalendar, SharedCalendarError>>
}
