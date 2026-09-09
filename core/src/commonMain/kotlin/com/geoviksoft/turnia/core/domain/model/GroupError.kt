package com.geoviksoft.turnia.core.domain.model

enum class GroupError {
    NotFound,
    LoadFailed,
    SaveFailed,

    /** Leaving would leave a group with members and nobody able to administer it. */
    LastAdmin,

    /** A group can only be deleted once its admin is the last one in it. */
    NotEmpty,
}
