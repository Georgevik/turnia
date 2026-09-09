package com.georgevik.turnia.core.data.datasource.firestorefunctions

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.SharedCalendarFunction.Companion.MAX_RANGE_DAYS
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarRequest
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.SharedCalendarError
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.datetime.LocalDate

/**
 * Another user's whole calendar, groups included.
 */
class SharedCalendarFunction(private val functions: FirebaseFunctions) {

    /** [from] and [to] must be [MAX_RANGE_DAYS] apart at most, or the function refuses the call. */
    suspend fun getSharedCalendar(
        ownerId: UserId,
        from: LocalDate,
        to: LocalDate,
    ): Outcome<SharedCalendarResponse, SharedCalendarError> =
        outcomeCatching(TAG, { throwable -> throwable.toSharedCalendarError() }) {
            Logger.i(TAG, "Get shared calendar")
            trackFunction(FUNCTION_GET_SHARED_CALENDAR)
            val result = functions.httpsCallable(FUNCTION_GET_SHARED_CALENDAR)(
                SharedCalendarRequest(
                    ownerUid = ownerId.value,
                    from = from.toString(),
                    to = to.toString(),
                )
            )

            result.data<SharedCalendarResponse>()
        }

    private fun Throwable.toSharedCalendarError(): SharedCalendarError {
        val code = message?.substringBefore(':')?.trim()?.toIntOrNull()
        Logger.e(TAG, "Shared calendar failed with code $code", this)

        return when (code) {
            CODE_NOT_SHARED -> SharedCalendarError.NotShared
            CODE_RANGE_TOO_WIDE -> SharedCalendarError.RangeTooWide
            else -> SharedCalendarError.LoadFailed
        }
    }

    companion object {
        private const val TAG = "SharedCalendarFunction"
        private const val FUNCTION_GET_SHARED_CALENDAR = "getSharedCalendar"
        private const val CODE_NOT_SHARED = 4003
        private const val CODE_RANGE_TOO_WIDE = 4004

        /** The cap the function enforces; asking for more is refused outright. */
        const val MAX_RANGE_DAYS = 92
    }
}
