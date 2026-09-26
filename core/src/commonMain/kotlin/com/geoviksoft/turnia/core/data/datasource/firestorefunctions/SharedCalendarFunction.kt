package com.geoviksoft.turnia.core.data.datasource.firestorefunctions

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.SharedCalendarFunction.Companion.MAX_RANGE_DAYS
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarRequest
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.sharedcalendar.mappers.SharedCalendarErrorMapper
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.datetime.LocalDate

/**
 * Another user's whole calendar, groups included.
 */
class SharedCalendarFunction(
    private val functions: FirebaseFunctions,
    private val errorMapper: SharedCalendarErrorMapper,
) {

    /** [from] and [to] must be [MAX_RANGE_DAYS] apart at most, or the function refuses the call. */
    suspend fun getSharedCalendar(
        ownerId: UserId,
        from: LocalDate,
        to: LocalDate,
        since: String?,
    ): Outcome<SharedCalendarResponse, SharedCalendarError> =
        outcomeCatching(TAG, errorMapper::map) {
            Logger.i(TAG, "Get shared calendar${if (since == null) "" else " since $since"}")
            trackFunction(FUNCTION_GET_SHARED_CALENDAR)
            val result = functions.httpsCallable(FUNCTION_GET_SHARED_CALENDAR)(
                SharedCalendarRequest(
                    ownerUid = ownerId.value,
                    from = from.toString(),
                    to = to.toString(),
                    since = since,
                )
            )

            result.data<SharedCalendarResponse>()
        }

    companion object {
        private const val TAG = "SharedCalendarFunction"
        private const val FUNCTION_GET_SHARED_CALENDAR = "getSharedCalendar"

        /** The cap the function enforces; asking for more is refused outright. */
        const val MAX_RANGE_DAYS = 92
    }
}
