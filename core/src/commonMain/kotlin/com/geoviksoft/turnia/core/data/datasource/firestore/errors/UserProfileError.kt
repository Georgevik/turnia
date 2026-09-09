package com.geoviksoft.turnia.core.data.datasource.firestore.errors

sealed class UserProfileError {
    data object NotFound : UserProfileError()
    data class LoadFailed(val error: Throwable) : UserProfileError()
}
