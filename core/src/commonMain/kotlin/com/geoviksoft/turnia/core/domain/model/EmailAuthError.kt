package com.geoviksoft.turnia.core.domain.model

sealed interface EmailAuthError {
    /**
     * Wrong password and unknown email alike: with email enumeration protection on, Firebase
     * answers both the same way on purpose, so the app cannot tell them apart either.
     */
    data object InvalidCredentials : EmailAuthError
    data object EmailInUse : EmailAuthError
    data object WeakPassword : EmailAuthError
    data object Failed : EmailAuthError
}
