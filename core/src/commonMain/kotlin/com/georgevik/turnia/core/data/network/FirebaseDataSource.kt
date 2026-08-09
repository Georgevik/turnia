package com.georgevik.turnia.core.data.network

import dev.gitlive.firebase.auth.FirebaseAuth

/**
 * Firebase data source. Google sign-in is handled by KMPAuth; use this for
 * further Firebase/Firestore access as the domain grows.
 */
class FirebaseDataSource(
    val firebaseAuth: FirebaseAuth,
)
