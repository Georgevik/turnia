package com.georgevik.turnia.core.data.auth

import com.georgevik.turnia.core.domain.model.GoogleSignInResult

/**
 * Data source that acquires a Google credential on the current platform.
 * Android is implemented in [AuthProviderAndroid]; iOS is implemented in Swift
 * (GoogleSignIn) and injected into Koin at startup.
 */
interface AuthProvider {
    suspend fun getGoogleToken(): GoogleSignInResult
}
