package com.georgevik.turnia.core.domain.model

data class GoogleSignInToken(
    val idToken: String,
    val accessToken: String?
)
