package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.session.SessionEvents
import com.georgevik.turnia.core.domain.session.clearOnSignOut
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Instant

/**
 * Interacts with Firestore: `users/{uid}/personalEventTypes`
 */
class PersonalEventTypesFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventTypeDocMapper: PersonalEventTypeDocMapper,
    private val userSyncFirestore: UserSyncFirestore,
    sessionEvents: SessionEvents,
    scope: CoroutineScope,
) {

    private val lastSeenUpdate = mutableMapOf<String, Instant>()

    init {
        sessionEvents.clearOnSignOut(scope) { lastSeenUpdate.clear() }
    }

    suspend fun get(
        uid: String,
        isHostUser: Boolean
    ): Outcome<List<PersonalEventType>, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            if (isHostUser) return@outcomeCatching cachedOrServer(uid)

            val sync = userSyncFirestore.get(uid)
            sync.errorOrNull()?.let { Logger.e(TAG, "Error reading sync updates", it.error) }
            val updatedAt = sync.valueOrNull()?.personalEventTypesUpdatedAt.toInstantOrNull()

            // Sin timestamp no hay nada contra lo que comparar: sirve caché, pero una caché vacía
            // puede significar que nunca bajamos los tipos de este usuario, no que no tenga.
            if (updatedAt == null) return@outcomeCatching cachedOrServer(uid)

            val seen = lastSeenUpdate[uid]
            if (seen != null && seen >= updatedAt) {
                return@outcomeCatching queryEventTypes(uid, Source.CACHE)
            }

            // Solo se registra si la bajada fue bien: si lanza, el próximo intento reintenta.
            queryEventTypes(uid, Source.SERVER).also { lastSeenUpdate[uid] = updatedAt }
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
            markTypesUpdated(uid)
        }

    suspend fun delete(uid: String, typeId: String): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            Logger.i(TAG, "Delete personal type document")
            firestore.collection(PATH_PERSONAL_TYPES(uid)).document(typeId).updateFields {
                PersonalEventTypeDocument.FIELD_IS_DELETED to true
            }
            markTypesUpdated(uid)
        }

    private suspend fun markTypesUpdated(uid: String) {
        userSyncFirestore.updatePersonalEventTypes(uid).errorOrNull()?.let { error ->
            Logger.e(TAG, "Error updating personal event types sync", error.error)
        }
    }

    private suspend fun cachedOrServer(uid: String): List<PersonalEventType> =
        queryEventTypes(uid, Source.CACHE).ifEmpty { queryEventTypes(uid, Source.SERVER) }

    private suspend fun queryEventTypes(uid: String, source: Source): List<PersonalEventType> {
        val snapshot = firestore.collection(PATH_PERSONAL_TYPES(uid)).get(source)
        Logger.i(
            TAG,
            "Personal event types. Cache: ${snapshot.metadata.isFromCache}. " +
                    "Amount: ${snapshot.documents.size}. " +
                    "Changes: ${snapshot.documentChanges.size}"
        )

        return snapshot.documents.map { personalEventTypeDocMapper.map(it) }
    }

    companion object {
        private const val TAG = "PersonalEventTypesFirestore"
        private fun PATH_PERSONAL_TYPES(uid: String) = "users/${uid}/personalEventTypes"
    }
}
