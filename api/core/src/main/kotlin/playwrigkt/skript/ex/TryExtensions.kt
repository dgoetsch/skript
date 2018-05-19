package playwrigkt.skript.ex

import org.funktionale.tries.Try

data class AggregateException(val errors: List<Throwable>): Exception()

fun <T: Any> List<Try<T>>.liftTry(): Try<List<T>> {
    val errors = this
            .mapNotNull { it.failed().toOption().orNull() }
            .flatMap {
                when {
                    it is AggregateException -> it.errors
                    else -> listOf(it)
                }
            }

    if(errors.isEmpty()) {
        return Try.Success(this.mapNotNull { it.toOption().orNull() })
    } else {
        return Try.Failure(AggregateException(errors))
    }
}

fun <K: Any, V: Any> Map<K, Try<V>>.lift(): Try<Map<K, V>> {
    val errors = this
            .mapNotNull { it.value.failed().toOption().orNull() }
            .flatMap {
                when {
                    it is AggregateException -> it.errors
                    else -> listOf(it)
                }
            }

    if(errors.isEmpty()) {
        return Try.Success(this
                .mapNotNull { entry -> entry.value.toOption().map { entry.key to it }.orNull() }
                .toMap())
    } else {
        return Try.Failure(AggregateException(errors))
    }
}