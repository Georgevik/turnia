package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable

enum class AppStore { GOOGLE_PLAY, APP_STORE }

/** Takes the user to where they can rate the app, in the store this build was installed from. */
interface StoreReview {
    val store: AppStore
    fun open()
}

@Composable
expect fun rememberStoreReview(): StoreReview
