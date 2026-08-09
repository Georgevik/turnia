package com.georgevik.turnia.core.domain.repo

import com.georgevik.turnia.core.data.network.FirebaseDataSource
import com.georgevik.turnia.core.domain.model.GoogleSignInToken

class AuthRepository(
    private val firebaseDataSource: FirebaseDataSource
) {

    suspend fun signWithGoogle(tokens: GoogleSignInToken) {
        firebaseDataSource.signInUser(tokens)
    }
}
