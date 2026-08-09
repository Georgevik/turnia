package com.georgevik.turnia.core.domain.model

/**
 * Outcome of a Google sign-in attempt: the token on success, or a categorized
 * [GoogleSignInError] on failure.
 */
sealed interface GoogleSignInResult {
    data class Success(val token: GoogleSignInToken) : GoogleSignInResult
    data class Failure(val error: GoogleSignInError) : GoogleSignInResult
}
