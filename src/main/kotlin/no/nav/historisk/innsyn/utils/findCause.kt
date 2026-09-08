package no.nav.historisk.innsyn.utils

fun <T : Exception>Exception.findCause(e: Class<T>): T? {
    var cause: Throwable? = this
    while (cause != null) {
        if (e.isAssignableFrom(cause.javaClass)) {
            return cause as T
        }
        cause = cause.cause
    }
    return null
}