package com.georgevik.turnia.core.data.datasource.firestore.mappers

import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventDocument
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.system.toYearMonth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlin.time.Instant

class PersonalEventMapper {

    fun map(event: PersonalEvent): PersonalEventDocument {
        return PersonalEventDocument(
            typeId = event.type.id.value,
            date = event.date.toString(),
            yearMonth = event.date.toYearMonth().toString(),
            notes = event.notes.takeIf { it?.isNotEmpty() == true }
        )
    }

    fun map(snapshot: DocumentSnapshot): DocHolder<PersonalEventDocument> {
        val doc = snapshot.data(PersonalEventDocument.serializer())
        return DocHolder(id = snapshot.reference.id, doc = doc)
    }

    fun map(holder: DocHolder<PersonalEventDocument>, types: Map<EventTypeId, PersonalEventType>): PersonalEvent? {
        val doc = holder.doc
        val type = types[EventTypeId(doc.typeId)] ?: return null

        return PersonalEvent(
            id = EventId(holder.id),
            type = type,
            date = Instant.parse(doc.date),
            notes = doc.notes.takeIf { it?.isNotEmpty() == true },
        )
    }
}
