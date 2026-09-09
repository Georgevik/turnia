package com.geoviksoft.turnia.ui.system.components.time

/**
 * Bridges the `HH:mm` strings the data layer stores (`EventType.startTime` / `endTime`) to the
 * text a [TTimeField] holds, and back. The field only
 * ever contains digits the user typed — the colon is derived from them.
 */

private const val TIME_DIGITS = 4

/**
 * Formats typed digits as `HH:mm`, dropping anything that is not a digit.
 *
 * Every position is clamped to what a real time allows — hour tens up to `2`, a `2x` hour up to
 * `23`, minute tens up to `5` — and a leading digit above `2` is read as an hour with an implicit
 * zero (`9` becomes `09`), so a complete entry can never be an impossible time.
 */
fun String.toTimeInput(): String {
    val typed = filter { it.isDigit() }
    val digits = if (typed.firstOrNull()?.let { it > '2' } == true) "0$typed" else typed

    val time = buildString {
        digits.take(TIME_DIGITS).forEachIndexed { index, digit ->
            val max = when (index) {
                0 -> '2'
                1 -> if (first() == '2') '3' else '9'
                2 -> '5'
                else -> '9'
            }
            if (digit > max) return@buildString
            append(digit)
        }
    }

    return if (time.length > 2) "${time.take(2)}:${time.drop(2)}" else time
}

/** Completes a partially typed field — `08` is 08:00 — or `null` when nothing was typed. */
fun String.toTimeOrNull(): String? {
    val digits = filter { it.isDigit() }.ifEmpty { return null }.padEnd(TIME_DIGITS, '0')
    return "${digits.take(2)}:${digits.drop(2)}"
}
