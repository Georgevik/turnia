package com.geoviksoft.turnia.e2e.infra

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

/*
 * On the CI emulator the app window never gets input focus (the IME refuses to show for the same
 * reason), and Espresso waits for focus before it does anything, so every Espresso call times out
 * there. Compose actions are dispatched to the views directly and do not care. These helpers do what
 * Espresso's pressBack and closeSoftKeyboard did, without going through the focused window.
 */

private val dismissibleSheet = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss) and
    hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsDialog))

/** Dismisses the bottom sheet on top if there is one, and otherwise goes back from the screen. */
internal fun ComposeTestRule.pressBack() {
    val sheets = onAllNodes(dismissibleSheet, useUnmergedTree = true)
    if (sheets.fetchSemanticsNodes().isNotEmpty()) {
        sheets.onFirst().performSemanticsAction(SemanticsActions.Dismiss)
    } else {
        onResumedActivity { (it as ComponentActivity).onBackPressedDispatcher.onBackPressed() }
    }
    waitForIdle()
}

internal fun ComposeTestRule.hideKeyboard() {
    onResumedActivity { activity ->
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
    }
    waitForIdle()
}

private fun onResumedActivity(block: (Activity) -> Unit) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val activity = ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(Stage.RESUMED)
            .single()
        block(activity)
    }
}
