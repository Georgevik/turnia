package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val user: User?
    val userSession: StateFlow<UserSession>
    suspend fun signOut()
}
