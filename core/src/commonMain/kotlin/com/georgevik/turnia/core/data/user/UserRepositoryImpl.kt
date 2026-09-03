package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UsernameFirestore
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.model.UsernameError
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.mapError
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
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
    private val remoteProfiles: UserPathFirestore,
    private val remotePrivate: UserPrivateFirestore,
    private val remoteUsernames: UsernameFirestore,
    private val userMapper: UserDocumentMapper,
    private val provisioner: UserProvisioner,
    private val scope: CoroutineScope
) : UserRepository {

    private var _user: User? = null
    override val loggedUser: User? get() = _user

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
        emit(UserSession.Authenticated(userMapper.map(firebaseUser)))

        // Firestore's own offline persistence serves this from disk when there is no network.
        val remoteUserResult = remoteProfiles.fetch(firebaseUser.uid)
        remoteUserResult.valueOrNull()?.let { fetchedUser ->
            Logger.i(TAG, "Success user info for users/<uid>")
            val subscription = remotePrivate.fetchSubscription(firebaseUser.uid).valueOrNull()
            val profile = provisioner.backfillUsername(firebaseUser.uid, fetchedUser)
            // Emit session with updated userinfo
            emit(UserSession.Authenticated(userMapper.map(firebaseUser, profile, subscription)))
            return@flow
        }

        remoteUserResult.errorOrNull()?.let { error ->
            when (error) {
                is UserProfileError.LoadFailed -> {
                    Logger.e(TAG, "Failed load user: ${error.error}")
                }

                UserProfileError.NotFound -> {
                    // A brand-new account has no subscription document: absent means free tier.
                    provisioner.create(firebaseUser).valueOrNull()?.let { created ->
                        emit(
                            UserSession.Authenticated(
                                userMapper.map(firebaseUser, created, subscription = null)
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun getCalendarsSharedWithMe(): Outcome<List<UserProfile>, Unit> {
        val uid = _user?.firebaseUid ?: return Unit.toFailure()

        return remoteProfiles.fetchCalendarsSharedWithMe(uid)
            .mapError { error -> Logger.e(TAG, "Failed load shared calendars: $error") }
    }

    override suspend fun updateUsername(username: String): Outcome<Unit, UsernameError> {
        if (!provisioner.isValidUsername(username)) return UsernameError.Invalid.toFailure()

        val user = _user ?: return UsernameError.SaveFailed.toFailure()
        val previous = user.username

        // Reserve before publishing: a taken username must fail without touching the profile.
        remoteUsernames.claim(username, user.firebaseUid, user.displayName.orEmpty())
            .errorOrNull()?.let { return it.toFailure() }

        remoteProfiles.updateUsername(user.firebaseUid, username).errorOrNull()?.let { error ->
            Logger.e(TAG, "Failed to update username: $error")
            remoteUsernames.release(username)
            return UsernameError.SaveFailed.toFailure()
        }

        if (previous.isNotBlank() && previous != username) remoteUsernames.release(previous)
        _user = user.copy(username = username)
        return Unit.toSuccess()
    }

    override suspend fun searchUsers(prefix: String): Outcome<List<UserProfile>, Unit> =
        remoteUsernames.search(prefix.trim().lowercase())
            .mapError { error -> Logger.e(TAG, "Failed username search: $error") }

    override suspend fun signOut() {
        auth.signOut()
    }

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
}
