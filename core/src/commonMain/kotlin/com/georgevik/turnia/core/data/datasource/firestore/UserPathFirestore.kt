package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.datasource.firestore.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Interacts with Firestore: `users/{uid}`
 */
class UserPathFirestore(
    private val firestore: FirebaseFirestore,
    private val mapper: UserDocumentMapper,
    private val personalEventTypeDocMapper: PersonalEventTypeDocMapper
) {

    suspend fun fetch(uid: String): Outcome<UserProfile, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid).get()
            Logger.d(TAG, "Fetch user from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)
            val userDocument = snapshot.data(UserDocument.serializer())
            mapper.map(uid, userDocument)
        }

    suspend fun update(uid: String, userPatched: UserDocument): Outcome<Unit, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update user document")
            firestore.collection(PATH_USER).document(uid).set(userPatched)
        }

    suspend fun personalTypes(uid: String): Outcome<List<PersonalEventType>, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            val snapshot = firestore.collection("${PATH_USER}/${uid}/personalEventTypes").get()
            Logger.i(
                TAG,
                "Personal types from cache: ${snapshot.metadata.isFromCache}. Amount: ${snapshot.documents.size}. Changes: ${snapshot.documentChanges.size}"
            )

            snapshot.documents.map { personalEventTypeDocMapper.map(it) }
        }

    suspend fun setPersonalType(
        uid: String,
        typeId: String,
        personalType: PersonalEventType
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            val doc = personalEventTypeDocMapper.map(personalType)
            Logger.i(TAG, "Set personal type document")
            firestore.collection("${PATH_USER}/${uid}/personalEventTypes").document(typeId)
                .set(doc)
        }

    companion object {
        private const val TAG = "UserProfileFirestore"
        private const val PATH_USER = "users"
    }
}
