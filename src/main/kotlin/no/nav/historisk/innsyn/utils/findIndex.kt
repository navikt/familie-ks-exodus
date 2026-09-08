package no.nav.historisk.innsyn.utils

fun <T>Iterable<T>.findIndex(p: (T) -> Boolean): Int? {
    var i = 0
    for (e in this) {
        if (p(e)) {
            return i
        }
        i += 1
    }
    return null
}

fun <T>Iterable<T>.findLastIndex(p: (T) -> Boolean): Int? {
    var last: Int? = null
    var i = 0
    for (e in this) {
        if (p(e)) {
            last = i
        }
        i += 1
    }
    return last
}

