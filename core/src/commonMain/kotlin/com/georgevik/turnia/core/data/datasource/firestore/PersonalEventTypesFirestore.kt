package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.toTimestamp
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Interacts with Firestore: `users/{uid}/personalEventTypes`
 */
@OptIn(ExperimentalAtomicApi::class)
class PersonalEventTypesFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventTypeDocMapper: PersonalEventTypeDocMapper,
    private val userSyncFirestore: UserSyncFirestore,
    scope: CoroutineScope
) {

    private val listeners = SharedListeners<UserId, List<PersonalEventType>>(scope)

    fun observe(uid: UserId): Flow<List<PersonalEventType>> =
        listeners.shared(uid) { snapshots(uid) }

    private fun snapshots(uid: UserId): Flow<List<PersonalEventType>> = flow {
        val cachedTypes = queryEventTypes(uid, null, Source.CACHE)

        var known = cachedTypes
        emit(known.toDomain())

        emitAll(userSyncFirestore.observe(uid).mapNotNull { sync ->
            val cacheUpdatedAt = known.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()

            val serverUpdatedAt =
                sync.personalEventTypesUpdatedAt.toInstantOrNull() ?: return@mapNotNull null

            val settled = cacheUpdatedAt == null || serverUpdatedAt <= cacheUpdatedAt
            if (settled) {
                return@mapNotNull known.toDomain()
            }

            val changed = queryEventTypes(
                uid,
                cacheUpdatedAt.toTimestamp(),
                Source.SERVER
            ).associateBy { it.id }.toMutableMap()

            val merged = known.map { cached -> changed.remove(cached.id) ?: cached }
            known = merged + changed.values
            known.toDomain()
        })
    }

    suspend fun set(
        uid: UserId, personalType: PersonalEventType
    ): Outcome<Unit, GenericFirestoreError> = outcomeCatching(TAG, { GenericFirestoreError(it) }) {
        val doc = personalEventTypeDocMapper.map(personalType)
        Logger.i(TAG, "Set personal type document")

        val batch = firestore.batch()
        batch.set(
            firestore.collection(PATH_PERSONAL_TYPES(uid.value)).document(personalType.id.value),
            doc,
        )
        val syncWrite = userSyncFirestore.writePersonalEventTypes(batch, uid)
        batch.commit()
        trackWrite(TAG)
        syncWrite.committed()
    }

    suspend fun delete(uid: UserId, typeId: EventTypeId): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.i(TAG, "Delete personal type document")

            val batch = firestore.batch()
            batch.updateFields(
                firestore.collection(PATH_PERSONAL_TYPES(uid.value)).document(typeId.value)
            ) {
                PersonalEventTypeDocument.FIELD_IS_DELETED to true
                PersonalEventTypeDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
            }
            val syncWrite = userSyncFirestore.writePersonalEventTypes(batch, uid)
            batch.commit()
            trackWrite(TAG)
            syncWrite.committed()
        }

    private suspend fun queryEventTypes(
        uid: UserId, sinceUpdateAt: Timestamp?, source: Source
    ): List<DocHolder<PersonalEventTypeDocument>> {
        val snapshot = firestore.collection(PATH_PERSONAL_TYPES(uid.value)).where {
            sinceUpdateAt?.let { PersonalEventTypeDocument.FIELD_UPDATE_AT greaterThan it }
        }.get(source).trackData(TAG)

        Logger.i(
            TAG,
            "Personal event types. Source: $source. " + "Amount: ${snapshot.documents.size}. " + "Changes: ${snapshot.documentChanges.size}"
        )

        return snapshot.documents.map { personalEventTypeDocMapper.map(it) }
    }

    private fun List<DocHolder<PersonalEventTypeDocument>>.toDomain(): List<PersonalEventType> =
        map { personalEventTypeDocMapper.map(it) }

    companion object {
        private const val TAG = "PersonalEventTypesFirestore"
        private fun PATH_PERSONAL_TYPES(uid: String) = "users/${uid}/personalEventTypes"
    }
}
