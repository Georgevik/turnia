package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.UserPathFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UsernameFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.UserProfileFunction
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.UserDocumentMapper
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.domain.repository.FcmDelegate
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.mapError
import com.geoviksoft.turnia.core.system.onSuccess
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrElse
import com.geoviksoft.turnia.core.system.valueOrNull
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
import kotlinx.coroutines.flow.onEach
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
    private val fcmDelegate: FcmDelegate,
    private val analytics: Analytics,
    scope: CoroutineScope
) : UserRepository, FcmDelegate by fcmDelegate {

    private val _userSession = MutableStateFlow<UserSession>(UserSession.Loading)
    override val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    override val loggedUserFlow: Flow<User> =
        _userSession.filterIsInstance(UserSession.Authenticated::class).map { it.user }

    override val loggedUser: User? get() = (_userSession.value as? UserSession.Authenticated)?.user

    init {
        scope.launch {
            auth.authStateChanged
                // authStateChanged also fires on token refresh; only a different account is a new session.
                .distinctUntilChangedBy { it?.uid }
                .onEach { firebaseUser -> analytics.setUser(firebaseUser?.uid?.let(::UserId)) }
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

        // Every launch, not only the first: a token is rotated by the system without asking, and
        // `arrayUnion` makes re-registering the one we already have cost nothing but the write.
        registerFcmToken(userId)

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

    override fun getCalendarsSharedWithMe(): Flow<Outcome<List<UserProfile>, Unit>> =
        loggedUserFlow
            .flatMapLatest { user -> remoteProfiles.fetchCalendarsSharedWithMe(user.id) }
            .map { outcome ->
                outcome.mapError { Logger.e(TAG, "Failed load shared calendars: $it") }
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
            resolved[sharedUid] ?: UserProfile(id = sharedUid, name = "", username = "")
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
        // Before `signOut`, while the uid is still known: afterwards there is no document to take
        // this device's token out of, and it would keep receiving pushes for the account.
        loggedUser?.id?.let { unregisterFcmToken(it) }
        auth.signOut()
    }

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
}
