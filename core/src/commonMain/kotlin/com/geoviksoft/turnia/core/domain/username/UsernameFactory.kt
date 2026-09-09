package com.geoviksoft.turnia.core.domain.username

import com.geoviksoft.turnia.core.domain.model.UserProfile
import kotlin.random.Random

/**
 * Owns every rule about what a username is: how one is derived from a name, whether one the user
 * typed is acceptable, and how many times to try again when the generated one is already taken.
 *
 * It holds no dependency on storage — claiming a username is a write, and lives with the writes.
 */
class UsernameFactory(private val random: Random = Random.Default) {

    fun create(name: String): String = generateUsername(name, random)

    fun isValid(username: String): Boolean = isValidUsername(username)

    /**
     * The username a profile is missing, or `null` when it already has one. Users created before
     * usernames existed have none.
     */
    fun createForBackfill(profile: UserProfile): String? =
        if (profile.username.isBlank()) create(profile.name) else null

    companion object {
        /** A clash only costs new random digits, so a couple of retries make one vanishingly rare. */
        const val CLAIM_ATTEMPTS = 5
    }
}
