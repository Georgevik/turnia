package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventTypeDocMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.toTimestamp
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Instant

/**
 * Interacts with Firestore: `users/{uid}/personalEventTypes`
 */
@OptIn(ExperimentalAtomicApi::class)
class PersonalEventTypesFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventTypeDocMapper: PersonalEventTypeDocMapper,
    private val userSyncFirestore: UserSyncFirestore,
) {

    /**
     * The marker value a merge already answered.
     *
     * A marker written a round trip after the document it announces — as this class did before it
     * started batching both into one commit — stays newer than anything it has to say, so the
     * delta query comes back empty and the cache never catches up with it. Remembering which
     * marker was already reconciled makes that cost one query per session instead of one per read;
     * a marker that moves is a real change and is fetched as ever.
     */
    private val reconciled = AtomicReference(emptyMap<UserId, Instant>())

    suspend fun get(uid: UserId): Outcome<List<PersonalEventType>, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val cachedTypes = queryEventTypes(uid, null, Source.CACHE)
            val cacheUpdatedAt = cachedTypes.mapNotNull { it.doc.updateAt.toInstantOrNull() }
                .maxOrNull()

            if (cacheUpdatedAt == null) {
                // No cache -> Fetch from Server
                return@outcomeCatching queryEventTypes(uid, null, Source.SERVER).toDomain()
            }

            val sync = userSyncFirestore.get(uid)
            sync.errorOrNull()?.let { Logger.e(TAG, "Error reading sync updates", it.error) }
            val serverUpdatedAt = sync.valueOrNull()?.personalEventTypesUpdatedAt.toInstantOrNull()

            val settled = serverUpdatedAt == null ||
                serverUpdatedAt <= cacheUpdatedAt ||
                reconciled.load()[uid] == serverUpdatedAt
            if (settled) {
                return@outcomeCatching cachedTypes.toDomain()
            }

            val changed = queryEventTypes(uid, cacheUpdatedAt.toTimestamp(), Source.SERVER)
                .associateBy { it.id }
                .toMutableMap()
            markReconciled(uid, serverUpdatedAt)

            val merged = cachedTypes.map { cached -> changed.remove(cached.id) ?: cached }
            (merged + changed.values).toDomain()
        }

    suspend fun set(
        uid: UserId,
        personalType: PersonalEventType
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val doc = personalEventTypeDocMapper.map(personalType)
            Logger.i(TAG, "Set personal type document")

            val batch = firestore.batch()
            batch.set(
                firestore.collection(PATH_PERSONAL_TYPES(uid.value))
                    .document(personalType.id.value),
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

    private fun markReconciled(uid: UserId, updatedAt: Instant) {
        while (true) {
            val current = reconciled.load()
            if (reconciled.compareAndSet(current, current + (uid to updatedAt))) return
        }
    }

    private suspend fun queryEventTypes(
        uid: UserId,
        sinceUpdateAt: Timestamp?,
        source: Source
    ): List<DocHolder<PersonalEventTypeDocument>> {
        val snapshot = firestore.collection(PATH_PERSONAL_TYPES(uid.value)).where {
            sinceUpdateAt?.let { PersonalEventTypeDocument.FIELD_UPDATE_AT greaterThan it }
        }.get(source).trackData(TAG)

        Logger.i(
            TAG,
            "Personal event types. Source: $source. " +
                    "Amount: ${snapshot.documents.size}. " +
                    "Changes: ${snapshot.documentChanges.size}"
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
