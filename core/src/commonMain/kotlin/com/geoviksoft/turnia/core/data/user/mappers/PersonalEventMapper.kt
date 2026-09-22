package com.geoviksoft.turnia.core.data.user.mappers

import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.PersonalEventDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.PersonalOneOffDocument
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.system.parseEventDate
import com.geoviksoft.turnia.core.system.parseEventDateTime
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.datetime.yearMonth

class PersonalEventMapper {

    fun map(event: PersonalTypedEvent): PersonalEventDocument {
        return PersonalEventDocument(
            typeId = event.type.id.value,
            date = event.date.toString(),
            yearMonth = event.date.yearMonth.toString(),
            notes = event.notes.takeIf { it?.isNotEmpty() == true },
        )
    }

    fun map(event: PersonalOneOffEvent): PersonalOneOffDocument {
        return PersonalOneOffDocument(
            name = event.name,
            color = event.color,
            start = event.start.toString(),
            end = event.end.toString(),
            allDay = event.allDay,
            notes = event.notes.takeIf { it?.isNotEmpty() == true },
            yearMonthStart = event.start.date.yearMonth.toString(),
            yearMonthEnd = event.end.date.yearMonth.toString()
        )
    }


    fun map(snapshot: DocumentSnapshot): DocHolder<PersonalEventDocument> {
        val doc = snapshot.data(PersonalEventDocument.serializer())
        return DocHolder(id = snapshot.reference.id, doc = doc)
    }

    fun mapOneOff(snapshot: DocumentSnapshot): DocHolder<PersonalOneOffDocument> {
        val doc = snapshot.data(PersonalOneOffDocument.serializer())
        return DocHolder(id = snapshot.reference.id, doc = doc)
    }

    fun map(
        holder: DocHolder<PersonalEventDocument>,
        types: Map<EventTypeId, PersonalEventType>
    ): PersonalTypedEvent? {
        val doc = holder.doc
        val type = types[EventTypeId(doc.typeId)] ?: return null

        return PersonalTypedEvent(
            id = EventId(holder.id),
            type = type,
            date = parseEventDate(doc.date) ?: return null,
            notes = doc.notes.takeIf { it?.isNotEmpty() == true },
        )
    }

    fun map(holder: DocHolder<PersonalOneOffDocument>): PersonalOneOffEvent? {
        val doc = holder.doc

        return PersonalOneOffEvent(
            id = EventId(holder.id),
            name = doc.name,
            notes = doc.notes.takeIf { it?.isNotEmpty() == true },
            start = parseEventDateTime(doc.start) ?: return null,
            end = parseEventDateTime(doc.end) ?: return null,
            allDay = doc.allDay,
            color = doc.color
        )
    }
}
