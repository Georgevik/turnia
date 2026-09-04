package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UsernameFirestore
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.datasource.firestorefunctions.UserProfileFunction
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.model.UsernameError
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.mapError
import com.georgevik.turnia.core.system.onSuccess
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrElse
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImpl(
    private val auth: FirebaseAuth,
    private val remoteProfiles: UserPathFirestore,
    private val remotePrivate: UserPrivateFirestore,
    private val remoteUsernames: UsernameFirestore,
    private val userProfileFunction: UserProfileFunction,
    private val userMapper: UserDocumentMapper,
    private val provisioner: UserProvisioner,
    scope: CoroutineScope
) : UserRepository {

    private val _userSession = MutableStateFlow<UserSession>(UserSession.Loading)
    override val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    override val loggedUserFlow: Flow<User> = _userSession.filterIsInstance(UserSession.Authenticated::class).map { it.user }

    override val loggedUser: User? get() = (_userSession.value as? UserSession.Authenticated)?.user

    init {
        scope.launch {
            auth.authStateChanged
                // authStateChanged also fires on token refresh; only a different account is a new session.
                .distinctUntilChangedBy { it?.uid }
                .flatMapLatest { firebaseUser ->
                    if (firebaseUser == null) flowOf(UserSession.Unauthenticated)
                    else gatherUserInfo(firebaseUser)
                }.collect { session -> _userSession.value = session }
        }
    }

    private fun gatherUserInfo(firebaseUser: FirebaseUser): Flow<UserSession.Authenticated> = flow {
        val userId = UserId(firebaseUser.uid)
        // Emit what auth already knows so the UI is never blocked on the profile read.
        emit(UserSession.Authenticated(userMapper.map(firebaseUser)))

        // Firestore's own offline persistence serves this from disk when there is no network.
        val remoteUserResult = remoteProfiles.fetch(userId)
        remoteUserResult.valueOrNull()?.let { fetchedUser ->
            Logger.i(TAG, "Success user info for users/<uid>")
            val subscription = remotePrivate.fetchSubscription(userId).valueOrNull()
            val profile = provisioner.backfillUsername(userId, fetchedUser)
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
        val uid = loggedUser?.id ?: return Unit.toFailure()

        return remoteProfiles.fetchCalendarsSharedWithMe(uid)
            .mapError { error -> Logger.e(TAG, "Failed load shared calendars: $error") }
    }

    override suspend fun updateProfile(
        name: String,
        username: String
    ): Outcome<Unit, UsernameError> {
        if (!provisioner.isValidUsername(username)) return UsernameError.Invalid.toFailure()
        val user = loggedUser ?: return UsernameError.SaveFailed.toFailure()

        return userProfileFunction.updateProfile(name, username).onSuccess {
            _userSession.value = UserSession.Authenticated(
                user.copy(displayName = name, username = username)
            )
        }
    }

    override suspend fun searchUsers(prefix: String): Outcome<List<UserProfile>, Unit> =
        remoteUsernames.search(prefix.trim().lowercase())
            .mapError { error -> Logger.e(TAG, "Failed username search: $error") }

    override suspend fun getCalendarSharedWith(): Outcome<List<UserProfile>, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        val sharedUids = remoteProfiles.getUserDocument(uid).valueOrElse { error ->
            Logger.e(TAG, "Failed load the calendar grant list: $error")
            return Unit.toFailure()
        }.calendarSharedWith.map(::UserId)

        if (sharedUids.isEmpty()) return emptyList<UserProfile>().toSuccess()

        val resolved = remoteUsernames.findByUids(sharedUids).valueOrElse { error ->
            Logger.e(TAG, "Failed resolve the users shared with: $error")
            return Unit.toFailure()
        }.associateBy { it.id }

        return sharedUids.map { sharedUid ->
            resolved[sharedUid] ?: UserProfile(id = sharedUid, name = "Unknown", username = "")
        }.toSuccess()
    }

    override suspend fun grantCalendarAccess(userId: UserId): Outcome<Unit, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        return remoteProfiles.grantCalendarAccess(uid, userId)
            .mapError { error -> Logger.e(TAG, "Failed to grant calendar access: $error") }
    }

    override suspend fun revokeCalendarAccess(userId: UserId): Outcome<Unit, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        return remoteProfiles.revokeCalendarAccess(uid, userId)
            .mapError { error -> Logger.e(TAG, "Failed to revoke calendar access: $error") }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
}
