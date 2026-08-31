package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val userId: String?
    val userSession: StateFlow<UserSession>
    fun isUserLogged(): Boolean
    suspend fun signOut()
}
