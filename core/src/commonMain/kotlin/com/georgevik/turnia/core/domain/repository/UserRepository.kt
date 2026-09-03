package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.model.UsernameError
import com.georgevik.turnia.core.system.Outcome
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val loggedUser: User?
    val userSession: StateFlow<UserSession>
    suspend fun signOut()

    suspend fun getCalendarsSharedWithMe(): Outcome<List<UserProfile>, Unit>

    /** Replaces the auto-generated username with one the user chose. */
    suspend fun updateUsername(username: String): Outcome<Unit, UsernameError>

    /** Users whose username starts with [prefix]. Empty for a prefix that is too short. */
    suspend fun searchUsers(prefix: String): Outcome<List<UserProfile>, Unit>
}
