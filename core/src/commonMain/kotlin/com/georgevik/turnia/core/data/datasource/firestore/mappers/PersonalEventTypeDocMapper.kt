package com.georgevik.turnia.core.data.datasource.firestore.mappers

import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.PersonalEventType
import dev.gitlive.firebase.firestore.DocumentSnapshot

class PersonalEventTypeDocMapper {

    fun map(type: PersonalEventType): PersonalEventTypeDocument {
        return PersonalEventTypeDocument(
            name = type.name,
            color = type.color,
            acronym = type.acronym,
            description = type.description,
            startTime = type.startTime,
            endTime = type.endTime,
        )
    }

    fun map(snapshot: DocumentSnapshot): DocHolder<PersonalEventTypeDocument> = DocHolder(
        id = snapshot.reference.id,
        doc = snapshot.data(PersonalEventTypeDocument.serializer()),
    )

    fun map(holder: DocHolder<PersonalEventTypeDocument>): PersonalEventType {
        val doc = holder.doc

        return PersonalEventType(
            id = EventTypeId(holder.id),
            name = doc.name,
            color = doc.color,
            acronym = doc.acronym,
            description = doc.description,
            startTime = doc.startTime,
            endTime = doc.endTime,
            isDeleted = doc.isDeleted,
        )
    }
}
