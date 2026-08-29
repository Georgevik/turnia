package com.georgevik.turnia.navigation

import androidx.navigation3.runtime.NavKey

interface Navigator {
    fun goTo(route: NavKey)
    fun goBack()
}
