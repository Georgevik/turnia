package com.geoviksoft.turnia.core.data.group

import kotlin.uuid.Uuid

/**
 * Mints invitation codes. It knows nothing about who stores one or how long it lives — only what a
 * code is made of, which is why the length arrives as an argument instead of being read here.
 */
class InvitationCodeFactory {

    fun create(codeLength: Int): String {
        // The length arrives from a feature flag, so it is whatever the backend last said. Zero
        // would return an empty string and hand out a group nobody can join.
        val length = codeLength.coerceIn(CODE_LENGTHS)
        val unbiasedLimit = 256 - 256 % INVITATION_CODE_ALPHABET.length
        val code = StringBuilder(length)

        while (code.length < length) {
            for (byte in Uuid.random().toByteArray()) {
                val value = byte.toInt() and 0xFF
                if (value >= unbiasedLimit) continue

                code.append(INVITATION_CODE_ALPHABET[value % INVITATION_CODE_ALPHABET.length])
                if (code.length == length) break
            }
        }

        return code.toString()
    }

    companion object {
        private const val INVITATION_CODE_ALPHABET = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ"

        /** Not a policy — the range outside which the flag is certainly a mistake. */
        private val CODE_LENGTHS = 4..16
    }

}
