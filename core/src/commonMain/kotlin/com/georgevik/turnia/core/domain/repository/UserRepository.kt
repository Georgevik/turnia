package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.model.UsernameError
import com.georgevik.turnia.core.system.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository : FcmDelegate {
    val loggedUser: User?
    val userSession: StateFlow<UserSession>

    val loggedUserFlow: Flow<User>
    suspend fun signOut()

    fun getCalendarsSharedWithMe(): Flow<Outcome<List<UserProfile>, Unit>>

    suspend fun updateProfile(name: String, username: String): Outcome<Unit, UsernameError>

    suspend fun searchUsers(prefix: String): Outcome<List<UserProfile>, Unit>

    suspend fun getCalendarSharedWith(): Outcome<List<UserProfile>, Unit>

    suspend fun grantCalendarAccess(userId: UserId): Outcome<Unit, Unit>

    suspend fun revokeCalendarAccess(userId: UserId): Outcome<Unit, Unit>
}

interface FcmDelegate {
    suspend fun registerFcmToken(uid: UserId)

    suspend fun unregisterFcmToken(uid: UserId)
}
