package com.geoviksoft.turnia.e2e.infra

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Starts [TurniaTestApplication] in place of the app's own, so every test talks to the emulators. */
class TurniaTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TurniaTestApplication::class.java.name, context)
}
