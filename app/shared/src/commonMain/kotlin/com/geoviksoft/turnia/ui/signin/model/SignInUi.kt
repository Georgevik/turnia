package com.geoviksoft.turnia.ui.signin.model

data class SignInUi(
    val signingIn: Boolean = false,
    val userMessage: SignInError? = null,
    /** `null` while the email sheet is closed. */
    val emailForm: EmailForm? = null,
)

enum class SignInError { Failed }

data class EmailForm(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: EmailFormError? = null,
    val resetSentTo: String? = null,
)

enum class EmailFormError {
    InvalidEmail,
    PasswordRequired,
    InvalidCredentials,
    Failed,
}

// Only the shape Firebase would reject outright; whether the address exists is Firebase's to say.
private val EmailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun String.isEmailShaped(): Boolean = EmailShape.matches(trim())
