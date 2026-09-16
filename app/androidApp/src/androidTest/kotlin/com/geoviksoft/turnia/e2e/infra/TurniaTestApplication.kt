package com.geoviksoft.turnia.e2e.infra

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.geoviksoft.turnia.TurniaApplication
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import org.koin.core.module.Module

/**
 * The app pointed at the Firebase emulators, with App Check left out: the emulators do not ask for
 * it, and the debug provider would only log a token nobody registers.
 */
class TurniaTestApplication : TurniaApplication() {

    override fun onCreate() {
        // The robots match English text, whatever the device speaks.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        super.onCreate()
    }

    override fun installFirebase() {
        Firebase.auth.useEmulator(Emulator.HOST, Emulator.AUTH_PORT)
        Firebase.firestore.useEmulator(Emulator.HOST, Emulator.FIRESTORE_PORT)
        Firebase.functions(Emulator.FUNCTIONS_REGION).useEmulator(Emulator.HOST, Emulator.FUNCTIONS_PORT)
    }

    override fun extraModules(): List<Module> = listOf(e2eModule)
}
