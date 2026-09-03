package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.system.Outcome
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val loggedUser: User?
    val userSession: StateFlow<UserSession>
    suspend fun signOut()

    suspend fun getCalendarsSharedWithMe(): Outcome<List<UserProfile>, Unit>
}
