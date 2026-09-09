package com.geoviksoft.turnia.navigation

import androidx.navigation3.runtime.NavKey

interface Navigator {
    fun goTo(route: NavKey)
    fun goBack()
    fun popToRoot()
}
