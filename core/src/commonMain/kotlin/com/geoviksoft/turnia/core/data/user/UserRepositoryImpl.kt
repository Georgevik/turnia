package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.UserPathFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UsernameFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.UserProfileFunction
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.UserDocumentMapper
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.DeleteAccountError
import com.geoviksoft.turnia.core.domain.model.EmailAuthError
import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.domain.repository.FcmDelegate
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.core.system.mapError
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.core.system.onSuccess
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrElse
import com.geoviksoft.turnia.core.system.valueOrNull
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    /**
     * The name typed on the sign-up form. Auth has nowhere to carry it into the new session — the
     * account exists before a display name could be set on it — so it waits here for the profile
     * that session creates.
     */
    private var pendingSignUpName: String? = null
    override val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    override val loggedUserFlow: Flow<User> =
        _userSession.filterIsInstance(UserSession.Authenticated::class).map { it.user }
            .distinctUntilChangedBy { it.id }

    override val loggedUser: User? get() = (_userSession.value as? UserSession.Authenticated)?.user

    init {
        scope.launch {
            // A session restored at launch arrives first, with no sign-out before it; only a user
            // who follows a signed-out state actually signed in.
            var signedOutSeen = false
            auth.authStateChanged
                // authStateChanged also fires on token refresh; only a different account is a new session.
                .distinctUntilChangedBy { it?.uid }
                .onEach { firebaseUser -> analytics.setUser(firebaseUser?.uid?.let(::UserId)) }
                .flatMapLatest { firebaseUser ->
                    if (firebaseUser == null) {
                        signedOutSeen = true
                        flowOf(UserSession.Unauthenticated)
                    } else {
                        gatherUserInfo(firebaseUser, signedIn = signedOutSeen)
                    }
                }.collect { session -> _userSession.value = session }
        }
    }

    private fun gatherUserInfo(
        firebaseUser: FirebaseUser,
        signedIn: Boolean,
    ): Flow<UserSession.Authenticated> = flow {
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
            // An account found is an account that existed: a new one logs `sign_up` instead.
            if (signedIn) analytics.log(AnalyticsEvent.Login(signInMethod(firebaseUser)))
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
                    provisioner.create(
                        firebaseUser, pendingSignUpName.also { pendingSignUpName = null })
                        .valueOrNull()?.let { created ->
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
        loggedUserFlow.flatMapLatest { user -> remoteProfiles.fetchCalendarsSharedWithMe(user.id) }
            .map { outcome ->
                outcome.mapError { Logger.e(TAG, "Failed load shared calendars: $it") }
            }

    override fun getHiddenSharedCalendars(): Flow<Set<UserId>> =
        loggedUserFlow.flatMapLatest { user -> remotePrivate.observePreferences(user.id) }
            .map { pref ->
                pref.hiddenSharedCalendars.mapTo(mutableSetOf()) { userId -> UserId(userId) }
            }.distinctUntilChanged()

    override suspend fun hideSharedCalendar(userId: UserId): Outcome<Unit, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        return remotePrivate.hideSharedCalendar(uid, userId)
            .onSuccess { analytics.log(AnalyticsEvent.SharedCalendarHidden) }
            .mapError { error -> Logger.e(TAG, "Failed to hide a shared calendar: $error") }
    }

    override suspend fun unhideSharedCalendar(userId: UserId): Outcome<Unit, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        return remotePrivate.unhideSharedCalendar(uid, userId)
            .mapError { error -> Logger.e(TAG, "Failed to unhide a shared calendar: $error") }
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

    /**
     * The reservation is the index; the profile behind each hit comes from `users/{uid}`.
     *
     * Each hit carries the `updateAt` of its reservation, which the profile read compares its
     * cached copy against — so searching the same prefix twice costs the query and nothing more.
     */
    override suspend fun searchUsers(prefix: String): Outcome<List<UserProfile>, Unit> {
        val matches = remoteUsernames.search(prefix.trim().lowercase()).valueOrElse { error ->
            Logger.e(TAG, "Failed username search: $error")
            return Unit.toFailure()
        }

        return matches.distinctBy { it.uid }.mapNotNull { match ->
            remoteProfiles.fetchProfile(match.uid, match.updateAt).valueOrNull()
        }.toSuccess()
    }

    override suspend fun getProfiles(userIds: List<UserId>): Outcome<List<UserProfile>, Unit> =
        remoteProfiles.fetchProfiles(userIds)
            .mapError { error -> Logger.e(TAG, "Failed to read profiles: $error") }

    override suspend fun getCalendarSharedWith(): Outcome<List<UserProfile>, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        val sharedUids = remoteProfiles.getUserDocument(uid).valueOrElse { error ->
            Logger.e(TAG, "Failed load the calendar grant list: $error")
            return Unit.toFailure()
        }.calendarSharedWith.map(::UserId)

        if (sharedUids.isEmpty()) return emptyList<UserProfile>().toSuccess()

        val resolved = remoteProfiles.fetchProfiles(sharedUids).valueOrElse { error ->
            Logger.e(TAG, "Failed resolve the users shared with: $error")
            return Unit.toFailure()
        }.associateBy { it.id }

        return sharedUids.map { sharedUid ->
            resolved[sharedUid] ?: UserProfile(id = sharedUid, name = "", username = "")
        }.toSuccess()
    }

    override suspend fun updateAvatar(
        animalIconId: String?,
        backgroundColor: String?,
    ): Outcome<Unit, Unit> {
        val user = loggedUser ?: return Unit.toFailure()

        return remoteProfiles
            .updateAvatar(user.id, user.username, animalIconId, backgroundColor)
            .fold(
                onSuccess = {
                    _userSession.value = UserSession.Authenticated(
                        user.copy(
                            avatar = UserProfile.AnimalAvatar(animalIconId, backgroundColor)
                        )
                    )
                    Unit.toSuccess()
                },
                onFailure = { error ->
                    Logger.e(TAG, "Failed to save the avatar: $error")
                    Unit.toFailure()
                },
            )
    }

    override suspend fun enableShowAds(): Outcome<Unit, Unit> {
        val user = loggedUser ?: return Unit.toFailure()

        return remoteProfiles.enableShowAds(user.id, user.username)
            .onSuccess { _userSession.value = UserSession.Authenticated(user.copy(showAds = true)) }
            .mapError { error -> Logger.e(TAG, "Failed to enable ads: $error") }
    }

    override suspend fun grantCalendarAccess(userId: UserId): Outcome<Unit, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        return remoteProfiles.grantCalendarAccess(uid, userId)
            .onSuccess { analytics.log(AnalyticsEvent.CalendarShared) }
            .mapError { error -> Logger.e(TAG, "Failed to grant calendar access: $error") }
    }

    override suspend fun revokeCalendarAccess(userId: UserId): Outcome<Unit, Unit> {
        val uid = loggedUser?.id ?: return Unit.toFailure()

        return remoteProfiles.revokeCalendarAccess(uid, userId)
            .onSuccess { analytics.log(AnalyticsEvent.CalendarShareRevoked) }
            .mapError { error -> Logger.e(TAG, "Failed to revoke calendar access: $error") }
    }

    // Success returns nothing: `authStateChanged` picks the new user up and builds the session,
    // exactly as it does after Google or Apple.
    override suspend fun signInWithEmail(
        email: String, password: String
    ): Outcome<Unit, EmailAuthError> = outcomeCatching(TAG, ::toEmailAuthError) {
        auth.signInWithEmailAndPassword(email.trim(), password)
    }

    override suspend fun createAccountWithEmail(
        name: String,
        email: String,
        password: String,
    ): Outcome<Unit, EmailAuthError> {
        // Before the call: the session it opens reads the name as soon as the account exists.
        pendingSignUpName = name.trim()
        return outcomeCatching(TAG, ::toEmailAuthError) {
            auth.createUserWithEmailAndPassword(email.trim(), password)
            Unit
        }.onFailure { pendingSignUpName = null }
    }

    override suspend fun sendPasswordReset(email: String): Outcome<Unit, EmailAuthError> =
        outcomeCatching(TAG, ::toEmailAuthError) {
            auth.sendPasswordResetEmail(email.trim())
        }

    private fun toEmailAuthError(error: Throwable): EmailAuthError = when {
        error is FirebaseAuthUserCollisionException -> EmailAuthError.EmailInUse
        error is FirebaseAuthWeakPasswordException -> EmailAuthError.WeakPassword
        // The console's password policy is refused with a plain auth exception, told apart only by its code.
        error.message.orEmpty().contains(PASS_NOT_REQUIREMENTS) -> EmailAuthError.WeakPassword
        error is FirebaseAuthInvalidUserException -> EmailAuthError.InvalidCredentials
        // Also what a malformed address throws; the form checks the shape first, so here it is
        // almost always a wrong password.
        error is FirebaseAuthInvalidCredentialsException -> EmailAuthError.InvalidCredentials
        else -> EmailAuthError.Failed
    }

    override suspend fun signOut() {
        // Before `signOut`, while the uid is still known: afterwards there is no document to take
        // this device's token out of, and it would keep receiving pushes for the account.
        loggedUser?.id?.let { unregisterFcmToken(it) }
        auth.signOut()
    }

    override suspend fun deleteAccount(): Outcome<Unit, DeleteAccountError> =
        userProfileFunction.deleteAccount().onSuccess {
            // Before signing out, while the reports are still bound to the account being deleted.
            analytics.log(AnalyticsEvent.AccountDeleted)
            // Not `signOut()`: unregistering the push token writes to `private/account`, and the
            // session's token is still valid for a while, so it would recreate a document the
            // server has just deleted. There is nothing left to push to anyway.
            auth.signOut()
        }

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private const val PASS_NOT_REQUIREMENTS = "PASSWORD_DOES_NOT_MEET_REQUIREMENTS"
    }
}
