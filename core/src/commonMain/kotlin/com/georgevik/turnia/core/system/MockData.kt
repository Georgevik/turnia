package com.georgevik.turnia.core.system

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.uuid.Uuid

/** Builds a single mock group event type belonging to [groupId]. */
fun mockGroupType(
    groupId: String,
    name: String,
    acronym: String?,
    description: String?,
    startTime: String?,
    endTime: String?,
    swappable: Boolean = true,
): GroupEventType = GroupEventType(
    id = mockUuid(),
    groupId = groupId,
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
        GroupTypeSpec("Guardia noche", "GN", "Guardia de 12 horas en turno de noche.", "20:00", "08:00"),
        GroupTypeSpec("Turno mañana", "M", "Turno de mañana en planta.", "08:00", "15:00"),
        GroupTypeSpec("Turno tarde", "T", "Turno de tarde en planta.", "15:00", "22:00"),
    ),
    listOf(
        GroupTypeSpec("Cirugía programada", "CP", "Sesión de cirugía programada.", "08:00", "14:00"),
        GroupTypeSpec("Guardia localizada", "GL", "Disponibilidad bajo llamada.", null, null),
        GroupTypeSpec("Consulta preoperatoria", "PRE", "Valoración previa a la intervención.", "09:00", "13:00"),
    ),
    listOf(
        GroupTypeSpec("Ronda matinal", "RM", "Ronda de pacientes a primera hora.", "07:00", "09:00"),
        GroupTypeSpec("Turno partido", "TP", "Jornada partida en planta.", "10:00", "18:00"),
        GroupTypeSpec("Refuerzo festivo", "RF", "Refuerzo en día festivo. No intercambiable.", "09:00", "21:00", swappable = false),
    ),
)
val MOCK_GROUPS: List<Group> by lazy {
    GROUP_TYPE_SETS.map { specs ->
        val groupId = mockUuid()
        Group(
            id = groupId,
            name = mockGroupName(),
            types = specs.map { spec ->
                mockGroupType(
                    groupId = groupId,
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
        id = mockUuid(),
        name = "Vacaciones",
        color = mockColor(),
        acronym = "V",
        description = "Día de vacaciones.",
        startTime = null,
        endTime = null,
    ),
    PersonalEventType(
        id = mockUuid(),
        name = "Cita médica",
        color = mockColor(),
        acronym = "CM",
        description = "Cita con el médico.",
        startTime = "10:00",
        endTime = "11:00",
    ),
    PersonalEventType(
        id = mockUuid(),
        name = "Gimnasio",
        color = mockColor(),
        acronym = "G",
        description = "Entrenamiento en el gimnasio.",
        startTime = "18:00",
        endTime = "19:30",
    ),
    PersonalEventType(
        id = mockUuid(),
        name = "Cumpleaños",
        color = mockColor(),
        acronym = "C",
        description = null,
        startTime = null,
        endTime = null,
    ),
)

fun mockUuid() = Uuid.random().toString()

fun mockColor(): String {
    val randomInt = Random.nextInt(0x1000000)
    return "#" + randomInt.toString(radix = 16).padStart(length = 6, padChar = '0').uppercase()
}

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


fun mockGroupEvent(
    ownerId: String = mockUuid(),
    fromMonth: LocalDate,
    amount: Int = 15
): List<GroupEvent> {
    val firstOfMonth = LocalDate(fromMonth.year, fromMonth.month, 1)

    return (0 until amount).map {
        val group = MOCK_GROUPS.random()
        val groupType = group.types.random()
        val history = (0 until Random.nextInt(0, 3)).map { mockRealName() }

        GroupEvent(
            id = mockUuid(),
            groupId = group.id,
            ownerId = ownerId,
            assigneeId = if (history.isEmpty()) ownerId else mockUuid(),
            type = groupType,
            date = firstOfMonth.plus(Random.nextInt(0 until 30), DateTimeUnit.DAY),
            onSwap = Random.nextInt(5) == 1,
            colorHex = groupType.color,
            history = history
        )
    }
}

fun mockPersonalEvent(amount: Int = 15, fromMonth: LocalDate): List<PersonalEvent> {
    val firstOfMonth = LocalDate(fromMonth.year, fromMonth.month, 1)

    return (0 until amount).map {
        PersonalEvent(
            id = mockUuid(),
            type = MOCK_PERSONAL_TYPES.random(),
            date = firstOfMonth.plus(Random.nextInt(0 until 30), DateTimeUnit.DAY),
            notes = if (Random.nextInt(5) == 1) "Lorem ipsum dolor sit amet, consectetur adipiscing elit." else null,
        )
    }
}
