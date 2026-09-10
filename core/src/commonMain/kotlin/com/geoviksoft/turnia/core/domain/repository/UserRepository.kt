package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.system.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository : FcmDelegate {
    val loggedUser: User?
    val userSession: StateFlow<UserSession>

    val loggedUserFlow: Flow<User>
    suspend fun signOut()

    fun getCalendarsSharedWithMe(): Flow<Outcome<List<UserProfile>, Unit>>

    suspend fun updateProfile(name: String, username: String): Outcome<Unit, UsernameError>

    suspend fun updateAvatar(animalIconId: String?, backgroundColor: String?): Outcome<Unit, Unit>

    suspend fun searchUsers(prefix: String): Outcome<List<UserProfile>, Unit>

    suspend fun getProfiles(userIds: List<UserId>): Outcome<List<UserProfile>, Unit>

    suspend fun getCalendarSharedWith(): Outcome<List<UserProfile>, Unit>

    suspend fun grantCalendarAccess(userId: UserId): Outcome<Unit, Unit>

    suspend fun revokeCalendarAccess(userId: UserId): Outcome<Unit, Unit>
}

interface FcmDelegate {
    val notificationsEnabled: StateFlow<Boolean>

    suspend fun registerFcmToken(uid: UserId)

    suspend fun unregisterFcmToken(uid: UserId)

    suspend fun setNotificationsEnabled(uid: UserId, enabled: Boolean): Outcome<Unit, Unit>
}
