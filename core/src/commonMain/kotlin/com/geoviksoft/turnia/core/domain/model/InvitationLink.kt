package com.geoviksoft.turnia.core.domain.model

/**
 * The link a group's invitation code travels in: `https://HOST/PATH/CODE`.
 *
 * An https link and not the app's own scheme, because it is the only kind that still leads
 * somewhere when the app is not installed: the page behind it points at the store. That page opens
 * an installed app through `SCHEME://PATH/CODE`, so both forms are read back.
 *
 * The host, scheme and path live in [InvitationLinkConfig]. The host is a custom domain on Firebase
 * Hosting (`firebase/hosting`), which also serves the files that let Android and iOS open the https
 * form straight in the app.
 */
object InvitationLink {

    private const val HTTPS_PREFIX =
        "https://${InvitationLinkConfig.HOST}/${InvitationLinkConfig.PATH}/"
    private const val SCHEME_PREFIX =
        "${InvitationLinkConfig.SCHEME}://${InvitationLinkConfig.PATH}/"

    fun of(code: String): String = HTTPS_PREFIX + code

    /** The code [link] carries, or `null` when it is not an invitation link at all. */
    fun codeOf(link: String): String? {
        val tail = when {
            link.startsWith(HTTPS_PREFIX) -> link.removePrefix(HTTPS_PREFIX)
            link.startsWith(SCHEME_PREFIX) -> link.removePrefix(SCHEME_PREFIX)
            else -> return null
        }
        return codeOrNull(tail.substringBefore('?').substringBefore('#').trimEnd('/'))
    }

    /**
     * [value] as a code, or `null` if it cannot be one. A link or a store referrer is text anyone
     * can write, so only something shaped like a code reaches the join sheet; whether it names a
     * real group is for `requestToJoinGroup` to say.
     */
    fun codeOrNull(value: String): String? {
        val code = value.trim().uppercase()
        return if (CODE.matches(code)) code else null
    }

    private val CODE = Regex("[A-Z0-9]{4,16}")
}
