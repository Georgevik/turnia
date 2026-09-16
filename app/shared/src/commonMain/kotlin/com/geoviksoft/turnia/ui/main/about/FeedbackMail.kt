package com.geoviksoft.turnia.ui.main.about

object FeedbackMail {

    fun uri(address: String, subject: String, body: String) =
        "mailto:$address?subject=${encode(subject)}&body=${encode(body)}"

    /** RFC 6068 wants every reserved byte escaped, `+` included: mail apps do not read it as a space. */
    private fun encode(text: String) = buildString {
        text.encodeToByteArray().forEach { byte ->
            val c = byte.toInt().toChar()
            if (c.isLetterOrDigit() && c.code < 128 || c in "-._~") {
                append(c)
            } else {
                append('%')
                append(HEX[byte.toInt() shr 4 and 0xF])
                append(HEX[byte.toInt() and 0xF])
            }
        }
    }

    private const val HEX = "0123456789ABCDEF"
}
