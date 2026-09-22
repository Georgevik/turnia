package com.geoviksoft.turnia.core.data.sharedcalendar.mappers

import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCalendarMapperTest {

    // What `getSharedCalendar` returns for one-off events: `firebase/functions/src/sharedCalendar.ts`.
    private val response = """
        {
          "personalOneOffEvents": [
            {
              "eventId": "dentist",
              "name": "Dentista",
              "color": "#00897A",
              "start": "2026-09-24T17:30",
              "end": "2026-09-24T18:15",
              "allDay": false,
              "notes": "Llevar la tarjeta"
            },
            {
              "eventId": "congress",
              "name": "Congreso",
              "color": "#3949AB",
              "start": "2026-10-30T09:00",
              "end": "2026-11-01T18:00",
              "allDay": true,
              "notes": null
            }
          ]
        }
    """.trimIndent()

    private fun oneOffs(): List<PersonalOneOffEvent> =
        SharedCalendarMapper()
            .map(Json.decodeFromString(SharedCalendarResponse.serializer(), response))
            .personalEvents
            .filterIsInstance<PersonalOneOffEvent>()

    @Test
    fun readsATimedOneOffEvent() {
        assertEquals(
            PersonalOneOffEvent(
                id = EventId("dentist"),
                name = "Dentista",
                notes = "Llevar la tarjeta",
                start = LocalDateTime(LocalDate(2026, 9, 24), LocalTime(17, 30)),
                end = LocalDateTime(LocalDate(2026, 9, 24), LocalTime(18, 15)),
                allDay = false,
                color = "#00897A",
            ),
            oneOffs().first { it.id == EventId("dentist") },
        )
    }

    @Test
    fun readsAnAllDayEventAcrossMonths() {
        val congress = oneOffs().first { it.id == EventId("congress") }
        assertEquals(true, congress.allDay)
        assertEquals(LocalDate(2026, 10, 30), congress.start.date)
        assertEquals(LocalDate(2026, 11, 1), congress.end.date)
        assertEquals(null, congress.notes)
    }
}
