package com.georgevik.turnia.core.domain.model

/**
 * Categorized Google sign-in failures, so the UI can react per case.
 */
enum class GoogleSignInError {
    /** The user dismissed the sign-in dialog. */
    Cancelled,

    /** No Google account available, or the client ID / SHA-1 config does not match. */
    NoAccount,

    /** Any other, uncategorized failure. */
    Unknown,
}
