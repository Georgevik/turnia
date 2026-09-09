package com.georgevik.turnia.core.data.sharedcalendar.mappers

import com.georgevik.turnia.core.data.datasource.firestorefunctions.callableErrorCode
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.SharedCalendarError

class SharedCalendarErrorMapper {

    fun map(throwable: Throwable): SharedCalendarError {
        val code = throwable.callableErrorCode
        Logger.e(TAG, "Shared calendar failed with code $code", throwable)

        return when (code) {
            CODE_NOT_SHARED -> SharedCalendarError.NotShared
            CODE_RANGE_TOO_WIDE -> SharedCalendarError.RangeTooWide
            else -> SharedCalendarError.LoadFailed
        }
    }

    private companion object {
        const val TAG = "SharedCalendarErrorMapper"
        const val CODE_NOT_SHARED = 4003
        const val CODE_RANGE_TOO_WIDE = 4004
    }
}
