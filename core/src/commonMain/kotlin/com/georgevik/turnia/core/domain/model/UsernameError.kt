package com.georgevik.turnia.core.domain.model

sealed interface UsernameError {
    /** Not 3-20 characters of `a-z`, `0-9`, `_` or `.`. */
    data object Invalid : UsernameError

    /** Someone else already reserved it. */
    data object Taken : UsernameError
    data object SaveFailed : UsernameError
}
