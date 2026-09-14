package com.geoviksoft.turnia.core.domain.model

/**
 * The rules a new password has to meet. They mirror the password policy set in Firebase
 * Authentication, which is what actually enforces them: change one and the other goes with it.
 */
enum class PasswordRule(val isMetBy: (String) -> Boolean) {
    MinLength({ it.length >= PASSWORD_MIN_LENGTH }),
    Uppercase({ password -> password.any { it.isUpperCase() } }),
    Lowercase({ password -> password.any { it.isLowerCase() } }),
}

const val PASSWORD_MIN_LENGTH = 8

fun String.meetsPasswordPolicy(): Boolean = PasswordRule.entries.all { it.isMetBy(this) }
