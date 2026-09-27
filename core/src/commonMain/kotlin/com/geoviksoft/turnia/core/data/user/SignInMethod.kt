package com.geoviksoft.turnia.core.data.user

import dev.gitlive.firebase.auth.FirebaseUser

/**
 * The provider the account signs in with, in the words Google's `sign_up` and `login` reports
 * expect. Android lists Firebase's own `firebase` entry first, so it is skipped.
 */
internal fun signInMethod(firebaseUser: FirebaseUser): String =
    when (firebaseUser.providerData.firstOrNull { it.providerId != "firebase" }?.providerId) {
        "google.com" -> "google"
        "apple.com" -> "apple"
        "password" -> "email"
        else -> "other"
    }
