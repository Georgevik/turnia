package com.georgevik.turnia.core.domain.username

import kotlin.random.Random

private const val MAX_BASE_LENGTH = 12
private const val FALLBACK_BASE = "user"
const val USERNAME_MIN_LENGTH = 3
const val USERNAME_MAX_LENGTH = 20

// Folded by hand: kotlin common has no Unicode normalizer, and these are the letters a Spanish
// or Portuguese name actually brings.
private val ACCENTS = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ã' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ñ' to 'n', 'ç' to 'c',
)

/**
 * "Jorge González" → `jorgeg482`. The digits make a clash between two people with the same name
 * unlikely, not impossible: nothing here reserves the result (see `usernames` in the schema notes).
 */
fun generateUsername(name: String, random: Random = Random.Default): String {
    val words = name.lowercase().split(' ', '\t', '\n')
        .map { it.foldToUsername() }
        .filter { it.isNotEmpty() }

    val base = when {
        words.isEmpty() -> FALLBACK_BASE
        words.size == 1 -> words.first()
        else -> words.first() + words[1].first()
    }

    // 10..999, so two or three digits and never a leading zero that reads like a typo.
    return base.take(MAX_BASE_LENGTH) + random.nextInt(10, 1000)
}

fun isValidUsername(username: String): Boolean =
    username.length in USERNAME_MIN_LENGTH..USERNAME_MAX_LENGTH &&
            username.all { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '.' }

private fun String.foldToUsername(): String = buildString {
    this@foldToUsername.forEach { char ->
        val folded = ACCENTS[char] ?: char
        if (folded in 'a'..'z' || folded in '0'..'9') append(folded)
    }
}
