package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Interacts with Firestore: `users/{uid}/personalEventTypes`
 */
class PersonalEventTypesFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventTypeDocMapper: PersonalEventTypeDocMapper
) {

    suspend fun get(uid: String): Outcome<List<PersonalEventType>, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            val snapshot = firestore.collection(PATH_PERSONAL_TYPES(uid)).get()
            Logger.i(
                TAG,
                "Personal types from cache: ${snapshot.metadata.isFromCache}. Amount: ${snapshot.documents.size}. Changes: ${snapshot.documentChanges.size}"
            )

            snapshot.documents.map { personalEventTypeDocMapper.map(it) }
        }

    suspend fun set(
        uid: String,
        personalType: PersonalEventType
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            val doc = personalEventTypeDocMapper.map(personalType)
            Logger.i(TAG, "Set personal type document")
            firestore.collection(PATH_PERSONAL_TYPES(uid)).document(personalType.id)
                .set(doc)
        }

    suspend fun delete(uid: String, typeId: String): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            Logger.i(TAG, "Delete personal type document")
            firestore.collection(PATH_PERSONAL_TYPES(uid)).document(typeId).updateFields {
                PersonalEventTypeDocument.FIELD_IS_DELETED to true
            }
        }

    companion object {
        private const val TAG = "PersonalEventTypesFirestore"
        private fun PATH_PERSONAL_TYPES(uid: String) = "users/${uid}/personalEventTypes"
    }
}
