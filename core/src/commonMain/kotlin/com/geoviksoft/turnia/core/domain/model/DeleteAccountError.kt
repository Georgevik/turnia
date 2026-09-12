package com.geoviksoft.turnia.core.domain.model

sealed interface DeleteAccountError {
    /** The only admin of a group that still has members: the group needs someone to run it first. */
    data object LastAdmin : DeleteAccountError
    data object Failed : DeleteAccountError
}
