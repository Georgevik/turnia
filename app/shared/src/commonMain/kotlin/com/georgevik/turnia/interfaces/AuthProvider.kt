package com.georgevik.turnia.interfaces

import com.georgevik.turnia.core.domain.model.GoogleSignInResult

interface AuthProvider {
    suspend fun getGoogleToken(): GoogleSignInResult
}
