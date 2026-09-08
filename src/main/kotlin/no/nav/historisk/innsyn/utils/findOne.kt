package no.nav.historisk.innsyn.utils

inline fun<T> Iterable<T>.findAtMostOne(error: String? = null, predicate: (T) -> Boolean = { true }): T? {
    val err = error ?: "Forventet maksimalt ett element"

    val filtered = this.filter(predicate)

    if (filtered.size > 1) {
        throw FindOneException(err)
    }

    return filtered.firstOrNull()
}

inline fun<T> Iterable<T>?.findExactlyOne(error: String? = null, predicate: (T) -> Boolean = { true }): T {
    val err = error ?: "Forventet nøyaktig ett element"

    if (this == null) {
        throw FindOneException("$err (this == null)")
    }

    val filtered = this.filter(predicate)

    if (filtered.size != 1) {
        throw FindOneException(err)
    }

    return filtered.first()
}

class FindOneException(error: String) : RuntimeException(error)