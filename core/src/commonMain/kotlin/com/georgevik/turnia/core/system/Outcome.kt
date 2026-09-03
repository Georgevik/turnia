package com.georgevik.turnia.core.system

import kotlin.coroutines.cancellation.CancellationException

/**
 * A result type whose failure is an ordinary value rather than a [Throwable].
 *
 * `kotlin.Result` constrains failures to [Throwable], which forces every domain error to be modelled
 * as an exception: it allocates a stack trace nothing reads, and `getOrNull()` collapses the reason
 * for the failure into `null` at the first call site that is not careful. `Outcome` keeps the error
 * as a sealed type or enum, so callers `when` over it and the compiler checks every case is handled.
 */
sealed interface Outcome<out T, out E> {

    data class Success<out T>(val value: T) : Outcome<T, Nothing>

    data class Failure<out E>(val error: E) : Outcome<Nothing, E>
}

fun <T> T.toSuccess(): Outcome<T, Nothing> = Outcome.Success(this)

fun <E> E.toFailure(): Outcome<Nothing, E> = Outcome.Failure(this)

val Outcome<*, *>.isSuccess: Boolean get() = this is Outcome.Success

val Outcome<*, *>.isFailure: Boolean get() = this is Outcome.Failure

fun <T, E> Outcome<T, E>.valueOrNull(): T? = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> null
}

fun <T, E> Outcome<List<T>, E>.valueOrEmpty(): List<T> = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> emptyList()
}

fun <T, E> Outcome<T, E>.errorOrNull(): E? = when (this) {
    is Outcome.Success -> null
    is Outcome.Failure -> error
}

inline fun <T, E> Outcome<T, E>.valueOrElse(fallback: (E) -> T): T = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> fallback(error)
}

inline fun <T, E, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, E, F> Outcome<T, E>.mapError(transform: (E) -> F): Outcome<T, F> = when (this) {
    is Outcome.Success -> this
    is Outcome.Failure -> Outcome.Failure(transform(error))
}

inline fun <T, E, R> Outcome<T, E>.flatMap(transform: (T) -> Outcome<R, E>): Outcome<R, E> =
    when (this) {
        is Outcome.Success -> transform(value)
        is Outcome.Failure -> this
    }

inline fun <T, E, R> Outcome<T, E>.fold(onSuccess: (T) -> R, onFailure: (E) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}

inline fun <T, E> Outcome<T, E>.onSuccess(action: (T) -> Unit): Outcome<T, E> = apply {
    if (this is Outcome.Success) action(value)
}

inline fun <T, E> Outcome<T, E>.onFailure(action: (E) -> Unit): Outcome<T, E> = apply {
    if (this is Outcome.Failure) action(error)
}

/**
 * Bridges a throwing API — Firebase/GitLive calls genuinely throw — into a typed [Outcome] at the
 * data-layer boundary, so exceptions never travel further up than the repository that produced them.
 */
inline fun <T, E> outcomeCatching(mapError: (Throwable) -> E, block: () -> T): Outcome<T, E> =
    try {
        Outcome.Success(block())
    } catch (cancellation: CancellationException) {
        // Swallowing this would break structured concurrency: a cancelled coroutine has to stay
        // cancelled. `runCatching` gets this wrong, which is another reason not to use it here.
        throw cancellation
    } catch (throwable: Throwable) {
        Outcome.Failure(mapError(throwable))
    }

inline fun <T, E> Result<T>.toOutcome(mapError: (Throwable) -> E): Outcome<T, E> = fold(
    onSuccess = { Outcome.Success(it) },
    onFailure = { Outcome.Failure(mapError(it)) },
)
