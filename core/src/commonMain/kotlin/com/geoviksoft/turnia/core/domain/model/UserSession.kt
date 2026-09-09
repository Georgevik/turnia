package com.geoviksoft.turnia.core.domain.model

sealed interface UserSession {
    data object Loading : UserSession
    data class Authenticated(val user: User) : UserSession
    data object Unauthenticated : UserSession
}
