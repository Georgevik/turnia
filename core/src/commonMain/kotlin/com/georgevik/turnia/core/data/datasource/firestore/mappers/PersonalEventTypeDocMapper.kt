package com.georgevik.turnia.core.data.datasource.firestore.mappers

import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
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

    fun map(snapshot: DocumentSnapshot): PersonalEventType {
        val doc = snapshot.data(PersonalEventTypeDocument.serializer())

        return PersonalEventType(
            id = snapshot.reference.id,
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
