package com.georgevik.turnia.ui.signin.model

data class SignInUi(
    val signingIn: Boolean = false,
    val userMessage: SignInError? = null,
)

enum class SignInError { Failed }
