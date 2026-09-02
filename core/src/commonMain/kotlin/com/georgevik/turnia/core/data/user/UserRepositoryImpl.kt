package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.data.user.datasource.SubscriptionDocument
import com.georgevik.turnia.core.data.user.datasource.Tier
import com.georgevik.turnia.core.data.user.datasource.UserDocument
import com.georgevik.turnia.core.data.user.datasource.UserProfileError
import com.georgevik.turnia.core.data.user.datasource.UserProfileFirestore
import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImpl(
    private val auth: FirebaseAuth,
    private val remoteProfiles: UserProfileFirestore,
    private val userFactory: UserFactory,
    private val scope: CoroutineScope
) : UserRepository {

    private var _user: User? = null
    override val user: User? get() = _user

    override val userSession: StateFlow<UserSession> = auth.authStateChanged
        // authStateChanged also fires on token refresh; only a different account is a new session.
        .distinctUntilChangedBy { it?.uid }
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) flowOf(UserSession.Unauthenticated)
            else gatherUserInfo(firebaseUser).onEach { _user = it.user }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = UserSession.Loading
        )

    private fun gatherUserInfo(firebaseUser: FirebaseUser): Flow<UserSession.Authenticated> = flow {
        // Emit what auth already knows so the UI is never blocked on the profile read.
        emit(UserSession.Authenticated(userFactory.create(firebaseUser)))

        // Firestore's own offline persistence serves this from disk when there is no network.
        val remoteUserResult = remoteProfiles.fetch(firebaseUser.uid)
        remoteUserResult.valueOrNull()?.let { fetchedUser ->
            Logger.i(TAG, "Success user info for users/${firebaseUser.uid}")
            // Emit session with updated userinfo
            emit(UserSession.Authenticated(userFactory.create(firebaseUser, fetchedUser)))
            return@flow
        }

        remoteUserResult.errorOrNull()?.let { error ->
            when (error) {
                is UserProfileError.LoadFailed -> {
                    Logger.e(TAG, "Failed load user: ${error.error}")
                }

                UserProfileError.NotFound -> {
                    createUserInFirestore(firebaseUser)
                }
            }
        }
    }

    private suspend fun createUserInFirestore(firebaseUser: FirebaseUser) {
        Logger.w(TAG, "Empty users/${firebaseUser.uid}. New user ")

        remoteProfiles.update(
            firebaseUser.uid, UserDocument(
                name = firebaseUser.displayName.orEmpty(),
                email = firebaseUser.email.orEmpty(),
                fcmTokens = emptyList(),
                subscription = SubscriptionDocument(tier = Tier.FREE)
            )
        ).fold(
            onSuccess = { Logger.i(TAG, "Created users/${firebaseUser.uid}") },
            onFailure = {
                Logger.e(
                    TAG,
                    "Could not create users/${firebaseUser.uid}; "
                )
            }
        )
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
}
