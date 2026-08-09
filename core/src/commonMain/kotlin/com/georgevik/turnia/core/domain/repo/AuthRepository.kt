package com.georgevik.turnia.core.domain.repo

import com.georgevik.turnia.core.data.network.FirebaseDataSource

class AuthRepository(
    private val firebaseDataSource: FirebaseDataSource
) {

    suspend fun signWithGoogle() {

    }
}
