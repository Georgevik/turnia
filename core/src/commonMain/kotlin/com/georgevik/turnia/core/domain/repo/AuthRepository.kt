package com.georgevik.turnia.core.domain.repo

import com.georgevik.turnia.core.data.auth.AuthProvider
import com.georgevik.turnia.core.data.network.FirebaseDataSource
import com.georgevik.turnia.core.domain.model.GoogleSignInResult

class AuthRepository(
    private val firebaseDataSource: FirebaseDataSource,
    private val authProvider: AuthProvider,
) {

    /**
     * Runs the full Google sign-in: acquires the credential (platform-specific
     * [AuthProvider]) and, on success, signs the user into Firebase. Returns the
     * categorized [GoogleSignInResult].
     */
    suspend fun signInWithGoogle(): GoogleSignInResult {
        val result = authProvider.getGoogleToken()
        if (result is GoogleSignInResult.Success) {
            firebaseDataSource.signInUser(result.token)
        }
        return result
    }
}
