package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.Membership
import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserSession
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserRepository(
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope
) {
    val userSession: StateFlow<UserSession> = auth.authStateChanged
        .map { firebaseUser ->
            Logger.d(
                TAG,
                "authStateChanged. UID ${firebaseUser?.uid}. Email: ${firebaseUser?.email}"
            )

            if (firebaseUser != null) {
                UserSession.Authenticated(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        membership = Membership.FREE
                    )
                )
            } else {
                UserSession.Unauthenticated
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = UserSession.Loading
        )

    fun isUserLogged(): Boolean = auth.currentUser != null

    companion object {
        private const val TAG = "UserRepository"
    }
}
