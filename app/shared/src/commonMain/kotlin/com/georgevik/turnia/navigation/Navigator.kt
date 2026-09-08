package com.georgevik.turnia.navigation

import androidx.navigation3.runtime.NavKey

interface Navigator {
    fun goTo(route: NavKey)

    /**
     * Swaps the current destination for [route]. For a screen that has just turned into a
     * different one — a blank form that saved and became the thing it was creating — going back
     * from what follows must not land on the form again.
     */
    fun replace(route: NavKey)

    fun goBack()
    fun popToRoot()
}
