package com.georgevik.turnia.core.data.network

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration.Companion.seconds

class FirebaseDataSource(
    private val firebaseAuth: FirebaseAuth
) {

    private val userToken = Firebase.auth.idTokenChanged

    suspend fun authUser() {

//        val userToken = userToken.filterNotNull().timeout(10.seconds).single()
//
//        val credential = GoogleAuthProvider.credential(idToken = userToken.to, accessToken = null)
//        val authResult = Firebase.auth.signInWithCredential(credential)
//        authResult.user

    }
}
