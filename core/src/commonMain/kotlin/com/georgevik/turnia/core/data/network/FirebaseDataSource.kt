package com.georgevik.turnia.core.data.network

import com.georgevik.turnia.core.domain.model.GoogleSignInToken
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider

class FirebaseDataSource(
    private val firebaseAuth: FirebaseAuth
) {

    suspend fun signInUser(tokens: GoogleSignInToken) {
        val credential = GoogleAuthProvider.credential(tokens.idToken, tokens.accessToken)
        val authResult = firebaseAuth.signInWithCredential(credential)
        authResult.user

    }

    companion object {
        private const val TAG = "FirebaseDataSource"
    }
}
