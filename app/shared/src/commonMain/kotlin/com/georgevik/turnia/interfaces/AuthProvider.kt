package com.georgevik.turnia.interfaces

import com.georgevik.turnia.core.domain.model.GoogleSignInToken


interface AuthProvider {
    suspend fun getGoogleToken(): GoogleSignInToken?
}
