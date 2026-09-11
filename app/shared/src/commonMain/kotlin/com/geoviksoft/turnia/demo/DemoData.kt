package com.geoviksoft.turnia.demo

import com.geoviksoft.turnia.core.domain.model.EventHistoryEntry
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.GroupMember
import com.geoviksoft.turnia.core.domain.model.Membership
import com.geoviksoft.turnia.core.domain.model.PersonalEvent
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.system.toInstant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

internal data class DemoPerson(
    val id: UserId,
    val name: String,
    val username: String,
    val avatar: UserProfile.AnimalAvatar,
) {
    val profile: UserProfile get() = UserProfile(id, name, username, avatar)
}

private fun person(id: String, name: String, username: String, animal: String, color: String) =
    DemoPerson(UserId("demo-$id"), name, username, UserProfile.AnimalAvatar(animal, color))

internal object DemoPeople {
    val lucia = person("lucia", "Lucía Ferrer", "luciaferrer", "fox", "#00897B")
    val javier = person("javier", "Javier Ruiz", "javiruiz", "otter", "#1E88E5")
    val marta = person("marta", "Marta Gómez", "martagomez", "koala", "#D81B60")
    val carlos = person("carlos", "Carlos Navarro", "cnavarro", "penguin", "#3949AB")
    val elena = person("elena", "Elena Sanz", "elenasanz", "hedgehog", "#F9A825")
    val pablo = person("pablo", "Pablo Ortega", "pabloortega", "panda", "#43A047")
    val sofia = person("sofia", "Sofía Martín", "sofiamartin", "rabbit", "#8E24AA")
    val andres = person("andres", "Andrés Vidal", "andresvidal", "dolphin", "#00ACC1")
    val irene = person("irene", "Irene Molina", "irenemolina", "llama", "#F4511E")

    val all = listOf(lucia, javier, marta, carlos, elena, pablo, sofia, andres, irene)

    val me = User(
        id = lucia.id,
        email = "lucia@example.com",
        displayName = lucia.name,
        username = lucia.username,
        // Premium, so no ad ever lands in a screenshot.
        membership = Membership.PREMIUM,
        avatar = lucia.avatar,
    )
}

/**
 * Everything the demo shows, built around [today] so the screenshots always show the current month.
 *
 * The rotations are plain cycles; the swaps are placed on top of them relative to today, so there
 * is always a colleague asking for a swap in the coming weeks, somebody who already covered one of Lucía's
 * shifts, and a chain of three holders to show off the history.
 */
internal class DemoWorld(val today: LocalDate) {

    val urgencias = GroupId("demo-urgencias")
    val uci = GroupId("demo-uci")

    private val urgenciasName = "Urgencias"
    private val uciName = "UCI Pediátrica"

    private val manana = groupType(urgencias, urgenciasName, "m", "Mañana", "M", "08:00", "15:00", "#039BE5")
    private val tarde = groupType(urgencias, urgenciasName, "t", "Tarde", "T", "15:00", "22:00", "#FB8C00")
    private val noche = groupType(urgencias, urgenciasName, "n", "Noche", "N", "22:00", "08:00", "#5E35B1")
    private val guardia = groupType(urgencias, urgenciasName, "g", "Guardia 24 h", "G", "08:00", "08:00", "#E53935")
    private val refuerzo = groupType(uci, uciName, "r", "Refuerzo", "R", "10:00", "18:00", "#43A047")

    private val urgenciasRoster = with(DemoPeople) {
        listOf(lucia to 0, javier to 2, marta to 4, carlos to 6, elena to 8, pablo to 3, sofia to 7)
    }

    val groups: List<Group> = listOf(
        Group(
            id = urgencias,
            name = urgenciasName,
            types = listOf(manana, tarde, noche, guardia),
            members = urgenciasRoster.map { (p, _) ->
                GroupMember(p.id, p.name, p.username, isAdmin = p == DemoPeople.lucia || p == DemoPeople.javier)
            }.sortedByDescending { it.isAdmin },
            memberCount = urgenciasRoster.size,
            invitationCode = "URG7K2",
            autoApprove = false,
            isAdmin = true,
            color = "#E53935",
        ),
        Group(
            id = uci,
            name = uciName,
            types = listOf(refuerzo),
            members = with(DemoPeople) { listOf(andres, lucia, sofia, pablo, elena) }.map { p ->
                GroupMember(p.id, p.name, p.username, isAdmin = p == DemoPeople.andres)
            },
            memberCount = 5,
            invitationCode = "",
            autoApprove = false,
            isAdmin = false,
            color = "#8E24AA",
        ),
    )

    val formacion = PersonalEventType(
        id = EventTypeId("demo-formacion"),
        name = "Formación",
        color = "#00897B",
        acronym = "F",
        description = "Cursos y sesiones clínicas",
        startTime = null,
        endTime = null,
    )
    val vacaciones = PersonalEventType(
        id = EventTypeId("demo-vacaciones"),
        name = "Vacaciones",
        color = "#F9A825",
        acronym = "V",
        description = null,
        startTime = null,
        endTime = null,
    )
    val personalTypes = listOf(formacion, vacaciones)

    val groupEvents: List<GroupEvent>
    val personalEvents: List<PersonalEvent>

    init {
        val events = rotations().toMutableList()
        val personal = mutableListOf<PersonalEvent>()
        val lucia = DemoPeople.lucia

        fun luciaOn(date: LocalDate) = events.any { it.assigneeId == lucia.id && it.date == date }
        fun freeOn(date: LocalDate, among: List<DemoPerson>) =
            among.firstOrNull { p -> events.none { it.assigneeId == p.id && it.date == date } }
                ?: among.first()
        fun nextOf(type: GroupEventType, after: LocalDate) = events
            .filter { it.assigneeId == lucia.id && it.type == type && it.date > after }
            .minBy { it.date }
        fun replace(event: GroupEvent, with: GroupEvent) {
            events[events.indexOf(event)] = with
        }

        // Two of Lucía's own shifts still waiting for somebody to cover them.
        val requestedNight = nextOf(noche, today.plus(1, DateTimeUnit.DAY))
        replace(requestedNight, requestedNight.copy(onSwap = true))
        val requestedAfternoon = nextOf(tarde, requestedNight.date)
        replace(requestedAfternoon, requestedAfternoon.copy(onSwap = true))

        // One Javier already took, and one that went Lucía → Marta → Carlos.
        val coveredMorning = nextOf(manana, today)
        val coverer = freeOn(coveredMorning.date, with(DemoPeople) { listOf(javier, elena, pablo) })
        replace(coveredMorning, coveredMorning.handedTo(coverer))
        val chainedMorning = nextOf(manana, coveredMorning.date.plus(3, DateTimeUnit.DAY))
        val first = freeOn(chainedMorning.date, listOf(DemoPeople.marta, DemoPeople.sofia))
        val second = freeOn(chainedMorning.date, listOf(DemoPeople.carlos, DemoPeople.pablo) - first)
        replace(chainedMorning, chainedMorning.handedTo(first).handedTo(second))

        // A Saturday guardia Lucía took from Marta.
        (2..30).map { today.plus(it, DateTimeUnit.DAY) }
            .firstOrNull { it.dayOfWeek.ordinal >= 5 && !luciaOn(it) }
            ?.let { day -> events += event(DemoPeople.marta, guardia, day).handedTo(lucia) }

        // Each of these gets a day of its own, so no two land in the same cell or the same date.
        val claimed = mutableSetOf<LocalDate>()
        fun luciaFree(date: LocalDate) = !luciaOn(date) && date !in claimed

        // A course on a free day.
        val courseDay = (4..20).map { today.plus(it, DateTimeUnit.DAY) }.first(::luciaFree)
        claimed += courseDay
        personal += PersonalEvent(
            id = EventId("demo-course"),
            type = formacion,
            date = courseDay.toInstant(),
            notes = "Curso de RCP avanzada · Aula 3, 16:00",
        )

        // Colleagues asking for a swap, on days Lucía has free, so she could cover any of them.
        with(DemoPeople) { listOf(carlos to noche, elena to tarde, marta to manana, javier to tarde) }
            .forEach { (colleague, type) ->
                val shift = events
                    .filter { it.ownerId == colleague.id && it.assigneeId == colleague.id && it.type == type }
                    .filter { it.date > today && luciaFree(it.date) && !it.onSwap }
                    .minByOrNull { it.date } ?: return@forEach
                replace(shift, shift.copy(onSwap = true))
                claimed += shift.date
            }
        // Pablo took this one from Sofía and now needs it swapped again: the list shows where it came from.
        events
            .filter { it.assigneeId == DemoPeople.sofia.id && it.date > today.plus(6, DateTimeUnit.DAY) && luciaFree(it.date) }
            .minByOrNull { it.date }
            ?.let { relay ->
                replace(relay, relay.handedTo(DemoPeople.pablo).copy(onSwap = true))
                claimed += relay.date
            }

        // Lucía reinforces the paediatric ICU on two of her days off; Andrés needs one of his covered.
        val uciDays = (3..30).map { today.plus(it, DateTimeUnit.DAY) }.filter(::luciaFree)
        events += event(lucia, refuerzo, uciDays[0])
        events += event(lucia, refuerzo, uciDays[2])
        val andresRequest = uciDays[4]
        events += event(DemoPeople.andres, refuerzo, andresRequest).copy(onSwap = true)
        (-40..90 step 4).map { today.plus(it, DateTimeUnit.DAY) }
            .filter { it != andresRequest }
            .forEach { date -> events += event(DemoPeople.andres, refuerzo, date) }

        // A week of holidays next month, with no shifts in it.
        val holidayStart = LocalDate(today.year, today.month, 1).plus(1, DateTimeUnit.MONTH).plus(11, DateTimeUnit.DAY)
        (0..4).map { holidayStart.plus(it, DateTimeUnit.DAY) }.forEach { date ->
            events.removeAll { it.assigneeId == lucia.id && it.date == date }
            personal += PersonalEvent(EventId("demo-holiday-$date"), vacaciones, date.toInstant(), null)
        }

        groupEvents = events
        personalEvents = personal
    }

    /** Six on, four off — mañanas, tardes, noches — each member a few days out of step. */
    private fun rotations(): List<GroupEvent> {
        val cycle = listOf(manana, manana, tarde, tarde, noche, noche, null, null, null, null)
        val start = today.minus(45, DateTimeUnit.DAY)
        return (0..150).flatMap { offset ->
            val date = start.plus(offset, DateTimeUnit.DAY)
            val day = date.toEpochDays()
            urgenciasRoster.mapNotNull { (person, shift) ->
                cycle[((day + shift) % cycle.size).toInt()]?.let { type -> event(person, type, date) }
            }
        }
    }

    private fun event(person: DemoPerson, type: GroupEventType, date: LocalDate) = GroupEvent(
        id = EventId("demo-${type.id.value}-${person.username}-$date"),
        groupId = type.groupId,
        groupName = type.groupName,
        ownerId = person.id,
        assigneeId = person.id,
        assigneeName = person.name,
        type = type,
        date = date,
        onSwap = false,
        colorHex = type.color,
        history = listOf(EventHistoryEntry(person.id, person.name)),
    )

    private fun GroupEvent.handedTo(person: DemoPerson) = copy(
        assigneeId = person.id,
        assigneeName = person.name,
        onSwap = false,
        history = history + EventHistoryEntry(person.id, person.name),
    )

    private fun groupType(
        group: GroupId,
        groupName: String,
        id: String,
        name: String,
        acronym: String,
        start: String,
        end: String,
        color: String,
    ) = GroupEventType(
        id = EventTypeId("${group.value}-$id"),
        groupId = group,
        groupName = groupName,
        name = name,
        acronym = acronym,
        description = null,
        startTime = start,
        endTime = end,
        swappable = true,
        defaultColor = color,
        userColor = null,
    )
}
