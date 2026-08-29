package com.georgevik.turnia.core.system

import com.georgevik.turnia.core.domain.model.GroupEventType
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.uuid.Uuid

val MOCK_TYPES = listOf(
    GroupEventType(
        id = "gt-night",
        name = "Guardia noche",
        acronym = "GN",
        description = "Guardia de 12 horas en turno de noche.",
        startTime = "20:00",
        endTime = "08:00",
        swappable = true,
        colorHex = mockColor(),
        userColor = if (Random.nextBoolean()) mockColor() else null,
    ),
    GroupEventType(
        id = "gt-morning",
        name = "Turno mañana",
        acronym = "M",
        description = "Turno de mañana en planta.",
        startTime = "08:00",
        endTime = "15:00",
        swappable = true,
        colorHex = mockColor(),
        userColor = if (Random.nextBoolean()) mockColor() else null,
    ),
    GroupEventType(
        id = "gt-afternoon",
        name = "Turno tarde",
        acronym = "T",
        description = "Turno de tarde en planta.",
        startTime = "15:00",
        endTime = "22:00",
        swappable = true,
        colorHex = mockColor(),
        userColor = if (Random.nextBoolean()) mockColor() else null,
    ),
    GroupEventType(
        id = "gt-training",
        name = "Formación",
        acronym = "F",
        description = "Sesión de formación interna. No intercambiable.",
        startTime = "16:00",
        endTime = "18:00",
        swappable = false,
        colorHex = mockColor(),
        userColor = if (Random.nextBoolean()) mockColor() else null,
    ),
)

fun mockUuid() = Uuid.random().toString()
fun mockDate(year: Int = 2026) = LocalDate(year, (1..12).random(), (1..30).random())
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
