package com.georgevik.turnia.core.system

import com.georgevik.turnia.core.domain.model.EventHistoryEntry
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.model.UserId
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid


val MOCK_MY_ID = UserId(mockUuid())

/** Builds a single mock group event type belonging to [groupId]. */
fun mockGroupType(
    groupId: GroupId,
    groupName: String,
    name: String,
    acronym: String?,
    description: String?,
    startTime: String?,
    endTime: String?,
    swappable: Boolean = true,
): GroupEventType = GroupEventType(
    id = EventTypeId(mockUuid()),
    groupId = groupId,
    groupName = groupName,
    name = name,
    acronym = acronym,
    description = description,
    startTime = startTime,
    endTime = endTime,
    swappable = swappable,
    colorHex = mockColor(),
    userColor = if (Random.nextBoolean()) mockColor() else null,
)

private class GroupTypeSpec(
    val name: String,
    val acronym: String?,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
    val swappable: Boolean = true,
)

private val GROUP_TYPE_SETS: List<List<GroupTypeSpec>> = listOf(
    listOf(
        GroupTypeSpec(
            "Guardia noche",
            "GN",
            "Guardia de 12 horas en turno de noche.",
            "20:00",
            "08:00"
        ),
        GroupTypeSpec("Turno mañana", "M", "Turno de mañana en planta.", "08:00", "15:00"),
        GroupTypeSpec("Turno tarde", "T", "Turno de tarde en planta.", "15:00", "22:00"),
    ),
    listOf(
        GroupTypeSpec(
            "Cirugía programada",
            "CP",
            "Sesión de cirugía programada.",
            "08:00",
            "14:00"
        ),
        GroupTypeSpec("Guardia localizada", "GL", "Disponibilidad bajo llamada.", null, null),
        GroupTypeSpec(
            "Consulta preoperatoria",
            "PRE",
            "Valoración previa a la intervención.",
            "09:00",
            "13:00"
        ),
    ),
    listOf(
        GroupTypeSpec(
            "Ronda matinal",
            "RM",
            "Ronda de pacientes a primera hora.",
            "07:00",
            "09:00"
        ),
        GroupTypeSpec("Turno partido", "TP", "Jornada partida en planta.", "10:00", "18:00"),
        GroupTypeSpec(
            "Refuerzo festivo",
            "RF",
            "Refuerzo en día festivo. No intercambiable.",
            "09:00",
            "21:00",
            swappable = false
        ),
    ),
)
val MOCK_GROUPS: List<Group> by lazy {
    GROUP_TYPE_SETS.mapIndexed { index, specs ->
        val groupId = GroupId(mockUuid())
        val groupName = mockGroupName()
        Group(
            id = groupId,
            name = groupName,
            memberCount = Random.nextInt(4, 24),
            invitationCode = mockUuid().take(6).uppercase(),
            // The signed-in user only administers some of their groups, so the detail screen has
            // both an editable and a read-only case to render.
            isAdmin = index == 0,
            types = specs.map { spec ->
                mockGroupType(
                    groupId = groupId,
                    groupName = groupName,
                    name = spec.name,
                    acronym = spec.acronym,
                    description = spec.description,
                    startTime = spec.startTime,
                    endTime = spec.endTime,
                    swappable = spec.swappable,
                )
            },
        )
    }
}

val MOCK_PERSONAL_TYPES = listOf(
    PersonalEventType(
        id = EventTypeId(mockUuid()),
        name = "Vacaciones",
        color = mockColor(),
        acronym = "V",
        description = "Día de vacaciones.",
        startTime = null,
        endTime = null,
    ),
    PersonalEventType(
        id = EventTypeId(mockUuid()),
        name = "Cita médica",
        color = mockColor(),
        acronym = "CM",
        description = "Cita con el médico.",
        startTime = "10:00",
        endTime = "11:00",
    ),
    PersonalEventType(
        id = EventTypeId(mockUuid()),
        name = "Gimnasio",
        color = mockColor(),
        acronym = "G",
        description = "Entrenamiento en el gimnasio.",
        startTime = "18:00",
        endTime = "19:30",
    ),
    PersonalEventType(
        id = EventTypeId(mockUuid()),
        name = "Cumpleaños",
        color = mockColor(),
        acronym = "C",
        description = null,
        startTime = null,
        endTime = null,
    ),
)

fun mockUuid() = Uuid.random().toString()

fun mockColor(): String = ALL_COLORS.random()

fun mockRealName(): String {
    val first = firstNames.random()
    val last1 = lastNames.random()
    return "$first $last1"
}

private val firstNames = listOf(
    "Alejandro", "Sofia", "Mateo", "Lucia", "Daniel",
    "Elena", "Lucas", "Valeria", "Carlos", "Emma",
    "David", "Martina", "Hugo", "Paula", "Liam"
)

private val lastNames = listOf(
    "Garcia", "Rodriguez", "Gonzalez", "Fernandez", "Lopez",
    "Martinez", "Sanchez", "Perez", "Gomez", "Martin",
    "Jimenez", "Ruiz", "Hernandez", "Diaz", "Moreno"
)


private val adjectives = listOf(
    "Alpha", "Agile", "Apex", "Blue", "Bright", "Cloud", "Core", "Cyber",
    "Delta", "Dynamic", "Elite", "Global", "Infinity", "NextGen", "Nova",
    "Omega", "Peak", "Pixel", "Prime", "Quantum", "Shadow", "Stellar", "Sync", "Urban"
)

private val nouns = listOf(
    "Builders", "Collective", "Crew", "Devs", "Engineers", "Force", "Guild",
    "Hub", "Innovators", "Knights", "Lab", "League", "Network", "Pioneers",
    "Squad", "Studio", "Syndicate", "Team", "Tribe", "Vanguard", "Warriors", "Wizards"
)

/** Generates a composed name, e.g., "Alpha Crew", "Quantum Devs" */
fun mockGroupName(): String {
    val adj = adjectives.random()
    val noun = nouns.random()
    return "$adj $noun"
}


/** A mock person with a stable id and name. */
data class MockPerson(val id: UserId, val name: String)

/**
 * A fixed pool of 10 people reused across mock events (owners, assignees, transfer
 * history) so the same id always maps to the same name.
 *
 * `lazy` so it initializes on first use, after the name pools ([mockRealName] →
 * firstNames/lastNames) declared below are set (see MockData init-order note).
 */
val MOCK_PEOPLE: List<MockPerson> by lazy {
    (1..4).map { MockPerson(id = UserId(mockUuid()), name = mockRealName()) } + MockPerson(MOCK_MY_ID, "")
}

fun mockGenerateEvents(
    fromMonth: LocalDate,
    me: UserId,
    amount: Int = 30
): List<GroupEvent> {
    val firstOfMonth = LocalDate(fromMonth.year, fromMonth.month, 1)
    // The pool's stand-in for "me" becomes whoever is actually signed in, so the events the
    // calendar marks as mine are the ones this account owns.
    val people = MOCK_PEOPLE.map { if (it.id == MOCK_MY_ID) it.copy(id = me) else it }

    return (0 until amount).map {
        val group = MOCK_GROUPS.random()
        val groupType = group.types.random()
        val owner = people.random()

        val history = buildList {
            add(EventHistoryEntry(owner.id, owner.name))
            var remainPeople = people - owner

            (0 until Random.nextInt(0, 6)).forEach {
                val person = remainPeople.random()
                remainPeople = people - person
                add(EventHistoryEntry(userId = person.id, userName = person.name))
            }
        }

        // The current holder is the last one the event passed through, else the owner.
        val assignee = history.last()

        GroupEvent(
            id = EventId(mockUuid()),
            groupId = group.id,
            groupName = group.name,
            ownerId = owner.id,
            assigneeId = assignee.userId,
            assigneeName = assignee.userName,
            type = groupType,
            date = firstOfMonth.plus(Random.nextInt(0 until 30), DateTimeUnit.DAY),
            onSwap = Random.nextInt(5) == 1,
            colorHex = groupType.color,
            history = history
        )
    }
}


/** Stands in for network latency while the repositories are backed by mock data. */
suspend fun mockDelay() = delay(Random.nextLong(300, 1000).milliseconds)
