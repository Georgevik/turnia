package com.geoviksoft.turnia.e2e.infra

import com.google.firebase.FirebaseApp

/** Where `firebase emulators:exec` serves, as the Android emulator sees it. Ports: firebase/firebase.json. */
internal object Emulator {
    const val HOST = "10.0.2.2"
    const val AUTH_PORT = 9099
    const val FIRESTORE_PORT = 8080
    const val FUNCTIONS_PORT = 5001

    // Must match FUNCTIONS_REGION in core's DataModule: the emulator serves each callable under it.
    const val FUNCTIONS_REGION = "europe-southwest1"

    /** The emulators keep one namespace per project, and the client addresses the one in google-services.json. */
    val projectId: String get() = requireNotNull(FirebaseApp.getInstance().options.projectId)
}
