package com.georgevik.turnia.core.domain.model

enum class GroupError {
    NotFound,
    LoadFailed,
    SaveFailed,

    /** Leaving would leave a group with members and nobody able to administer it. */
    LastAdmin,
}
